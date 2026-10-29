package dev.alexk.async;


import dev.alexk.config.TaskDispatcherProperties;
import dev.alexk.domain.OrderRepository;
import dev.alexk.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;


@Slf4j
@Service
@AllArgsConstructor
public class TaskDispatcher {

    private final TaskEntityRepository taskRepository;
    private final OrderRepository orderRepository;
    private final TransactionTemplate txTemplate;
    private final TaskProcessor taskProcessor;
    private final ExecutorService taskDispatcherThreadPool;

    private final TaskDispatcherProperties properties;

    public void dispatch(TaskEntity task) {
        CompletableFuture
                .supplyAsync(() -> taskProcessor.processTask(task), taskDispatcherThreadPool)
                .thenAccept(result -> handleTaskExecuted(task, result))
                .exceptionally(ex ->  handleExceptionInTaskHappened(task, ex));
    }

    private Void handleExceptionInTaskHappened(TaskEntity task, Throwable ex) {
        log.error("Task failed with unexpected exception: taskId={}", task.getId(), ex);
        scheduleTaskRetry(task);
        log.error("Success processed task failure: taskId={}", task.getId(), ex);
        return null;
    }

    private void handleTaskExecuted(TaskEntity task, TaskProcessResponse processResponse) {
        log.info("Task executed: taskId={}, status={}",
                task.getId(),
                processResponse.taskStatus);
        switch (processResponse.taskStatus) {
            case SUCCEEDED -> handleTaskSucceeded(task, processResponse);
            case FAILED_RETRYABLE -> scheduleTaskRetry(task);
            case FAILED_NON_RETRYABLE -> handleTaskFailed(task, processResponse);
        }
        log.info("Success processed execution status: taskId={}, status={}",
                task.getId(), processResponse.taskStatus);
    }

    /**
     * Handle response from PaymentStub or WarehouseStub
     * @param task
     * @param processResponse
     */
    private void handleTaskSucceeded(TaskEntity task, TaskProcessResponse processResponse) {
        log.info("Handling succeeded task with taskId={}", task.getId());
        var orderId = task.getOrder().getId();

        if (processResponse.authResponseDto != null) {
            log.info("Updating order and task after payment authorize step, orderId={}", orderId);
            txTemplate.execute(status -> {
                taskRepository.save(task.toBuilder()
                        .inProcessing(false)
                        .nextAttemptAt(OffsetDateTime.now().plus(properties.getRetryDelay()))
                        .attempts(task.getAttempts() == null ? 1 : task.getAttempts() + 1)
                        .note(processResponse.authResponseDto.message())
                        .build());
                orderRepository
                        .findById(task.getOrder().getId())
                        .map(order -> {
                            order.setAuthorizedAmount(processResponse.authResponseDto.authorizedAmount());
                            order.setPaymentStatus(PaymentStatus.PENDING);
                            return orderRepository.save(order);
                        })
                        .orElseThrow(() -> new RuntimeException("Order with id=" + orderId + " not found"));
                return null;
            });
        }
        else if (processResponse.pricingResponseDto != null) {
            log.info("Updating order and task after payment reprice step, orderId={}", orderId);
            var finalAmount = processResponse.pricingResponseDto.finalAmount();
            var authorizedAmount = task.getOrder().getAuthorizedAmount();
            if (finalAmount.compareTo(authorizedAmount) > 0) {
                txTemplate.execute(status -> {
                    taskRepository.save(task.toBuilder()
                            .status(TaskStatus.FAILED_NON_RETRYABLE)
                            .inProcessing(false)
                            .nextAttemptAt(null)
                            .note("Price calculated by stub is greater than customer estimate")
                            .build());
                    orderRepository
                            .findById(orderId)
                            .map(order -> {
                                order.setPaymentStatus(PaymentStatus.PRICE_CHANGED_FAILED);
                                return orderRepository.save(order);
                            })
                            .orElseThrow(() -> new RuntimeException("Order with id=" + orderId + " not found"));
                    return null;
                });
            } else {
                txTemplate.execute(status -> {
                    taskRepository.save(task.toBuilder()
                            .inProcessing(false)
                            .nextAttemptAt(OffsetDateTime.now().plus(properties.getRetryDelay()))
                            .attempts(task.getAttempts() == null ? 1 : task.getAttempts() + 1)
                            .note(processResponse.pricingResponseDto.reason())
                            .build());
                    orderRepository
                            .findById(orderId)
                            .map(order -> {
                                order.setFinalAmount(processResponse.pricingResponseDto.finalAmount());
                                order.setPaymentStatus(PaymentStatus.PENDING);
                                return orderRepository.save(order);
                            })
                            .orElseThrow(() -> new RuntimeException("Order with id=" + orderId + " not found"));
                    return null;
                });
            }
        }
        else if (processResponse.captureResponseDto != null) {
            log.info("Updating order and task after payment capture step, orderId={}", orderId);
            txTemplate.execute(status -> {
                taskRepository.save(task.toBuilder()
                        .status(TaskStatus.SUCCEEDED)
                        .inProcessing(false)
                        .nextAttemptAt(null)
                        .note(processResponse.captureResponseDto.message())
                        .build());
                orderRepository
                        .findById(task.getOrder().getId())
                        .map(order -> {
                            order.setCapturedAmount(processResponse.captureResponseDto.capturedAmount());
                            order.setPaymentStatus(PaymentStatus.SUCCESS_PAID);
                            return orderRepository.save(order);
                        })
                        .orElseThrow(() -> new RuntimeException("Order with id=" + orderId + " not found"));
                return null;
            });
        }
    }


    private void scheduleTaskRetry(TaskEntity task) {
        log.info("Scheduling default retry for taskId={}", task.getId());

        if (task.getAttempts() >= properties.getMaxAttempts()) {
            log.error("Maximum number of retries reached: taskId={}", task.getId());
            taskRepository.save(task.toBuilder()
                    .status(TaskStatus.FAILED_NON_RETRYABLE)
                    .nextAttemptAt(null)
                    .build());
            return;
        }

        var nextAttemptAt = OffsetDateTime.now().plus(properties.getRetryDelay());
        taskRepository.save(task.toBuilder()
                .status(TaskStatus.FAILED_RETRYABLE)
                .inProcessing(false)
                .nextAttemptAt(nextAttemptAt)
                .build());
    }


    private void handleTaskFailed(TaskEntity task, TaskProcessResponse processResponse) {
        log.info("Handling failed task with id={}", task.getId());
        txTemplate.execute(status -> {
            taskRepository.save(task.toBuilder()
                    .status(TaskStatus.FAILED_NON_RETRYABLE)
                    .inProcessing(false)
                    .nextAttemptAt(null)
                    .note(getProcessResponseMessage(processResponse))
                    .build());
            orderRepository
                    .findById(task.getOrder().getId())
                    .map(order -> {
                        if (order.getAuthorizedAmount() == null) {
                            order.setPaymentStatus(PaymentStatus.AUTHORIZATION_FAILED);
                        } else if (order.getFinalAmount() == null) {
                            order.setPaymentStatus(PaymentStatus.PRICE_CHANGED_FAILED);
                        } else if (order.getCapturedAmount() == null) {
                            order.setPaymentStatus(PaymentStatus.CAPTURE_FAILED);
                        }
                        return orderRepository.save(order);
                    })
                    .orElseThrow(() -> new RuntimeException("Order with id=" +
                            task.getOrder().getId() + " not found"));
            return null;
        });
    }

    private String getProcessResponseMessage(TaskProcessResponse processResponse) {
        String result = "";
        if (processResponse.authResponseDto != null) {
            result = processResponse.authResponseDto.message();
        } else if (processResponse.pricingResponseDto != null) {
            result = processResponse.pricingResponseDto.reason();
        } else if (processResponse.captureResponseDto != null) {
            result = processResponse.captureResponseDto.message();
        } else if (processResponse.message != null) {
            result = processResponse.message;
        }
        return result;
    }

}
