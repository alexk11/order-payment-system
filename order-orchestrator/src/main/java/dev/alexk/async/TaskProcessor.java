package dev.alexk.async;


import dev.alexk.payment.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import dev.alexk.config.PaymentWebClient;
import dev.alexk.warehouse.CalculatePricingRequestDto;
import dev.alexk.warehouse.CalculatePricingResponseDto;


@Slf4j
@Component
@AllArgsConstructor
public class TaskProcessor {

    private final PaymentWebClient webClient;
    private final ExecutorService externalHttpThreadPool;

    static final String PAYMENT_AUTHORIZE = "/payment/authorize";
    static final String PAYMENT_CAPTURE = "/payment/capture";
    static final String WAREHOUSE = "/warehouse/calculate-price";

    public TaskProcessResponse processTask(TaskEntity task) {
        log.info("Process task with id={}", task.getId());

        var result = new TaskProcessResponse();

        var order = task.getOrder();
        if (order == null) {
            log.error("Order for task with id={} doesn't exist", task.getId());
            result.taskStatus = TaskStatus.FAILED_NON_RETRYABLE;
            return result;
        }

        if (order.getAuthorizedAmount() == null) {
            var authDto =
                    new AuthorizePaymentRequestDto(
                            order.getCustomerId(),
                            order.getClientEstimate());
            callPaymentAuthStub(task.getId(), authDto, result);
        }
        else if (order.getFinalAmount() == null) {
            var repriceDto =
                    new CalculatePricingRequestDto(order.getId());
            callWarehouseStub(task.getId(), repriceDto, result);
        }
        else if (order.getCapturedAmount() == null) {
            var captureDto =
                    new CapturePaymentRequestDto(
                            order.getFinalAmount(),
                            order.getCustomerId());
            callPaymentCaptureStub(task.getId(), captureDto, result);
        }

        return result;
    }


    private void callPaymentAuthStub(
            Long taskId,
            AuthorizePaymentRequestDto requestDto,
            TaskProcessResponse processResponse) {

        log.info("Calling auth stub for taskId={}", taskId);
        var future = CompletableFuture.supplyAsync(() ->
                        webClient.webClientWithTimeout().post()
                                .uri(PAYMENT_AUTHORIZE)
                                .bodyValue(requestDto)
                                .retrieve()
                                .toEntity(AuthorizePaymentResponseDto.class)
                                .block(), // synchronous call
                externalHttpThreadPool);

        try {
            var authResponse = future.get();
            log.info("AUTH call completed with result {}", authResponse);
            if (authResponse.getBody() != null) {
                if (authResponse.getBody().authorizationId() != null) {
                    processResponse.taskStatus = TaskStatus.SUCCEEDED;
                }
                else if (authResponse.getBody().status() == AuthorizationStatus.DECLINED) {
                    processResponse.taskStatus = TaskStatus.FAILED_NON_RETRYABLE;
                }
                processResponse.authResponseDto = authResponse.getBody();
            }
        } catch (ExecutionException e) {
            log.error("Error while processing task with taskId={}", taskId, e);
            processResponse.taskStatus = TaskStatus.FAILED_NON_RETRYABLE;
            processResponse.message = e.getMessage();  // e.g. connection to payment stub refused
        } catch (InterruptedException e) {
            log.error("Thread was interrupted", e);
            Thread.currentThread().interrupt();
            processResponse.taskStatus = TaskStatus.FAILED_RETRYABLE;
        }
    }


    private void callWarehouseStub(
            Long taskId,
            CalculatePricingRequestDto requestDto,
            TaskProcessResponse processResponse) {

        log.info("Calling warehouse stub for taskId={}", taskId);
        var future = CompletableFuture.supplyAsync(() ->
                        webClient.webClientWithTimeout().post()
                                .uri(WAREHOUSE)
                                .bodyValue(requestDto)
                                .retrieve()
                                .toEntity(CalculatePricingResponseDto.class)
                                .block(), // synchronous call
                externalHttpThreadPool);

        try {
            var repriceResponse = future.get();
            log.info("REPRICE call completed: {}", repriceResponse);
            if (repriceResponse.getBody() != null) {
                if (repriceResponse.getBody().finalAmount() != null) {
                    processResponse.taskStatus = TaskStatus.SUCCEEDED;
                } else {
                    processResponse.taskStatus = TaskStatus.FAILED_RETRYABLE;
                }
                processResponse.pricingResponseDto = repriceResponse.getBody();
            }
        } catch (ExecutionException e) {
            log.error("Error while processing task with taskId={}", taskId, e);
            processResponse.taskStatus = TaskStatus.FAILED_NON_RETRYABLE;
            processResponse.message = e.getMessage();
        } catch (InterruptedException e) {
            log.error("Thread was interrupted", e);
            Thread.currentThread().interrupt();
            processResponse.taskStatus = TaskStatus.FAILED_RETRYABLE;
        }
    }


    private void callPaymentCaptureStub(
            Long taskId,
            CapturePaymentRequestDto requestDto,
            TaskProcessResponse processResponse) {

        log.info("Calling capture stub for taskId={}", taskId);
        var future = CompletableFuture.supplyAsync(() ->
                        webClient.webClientWithTimeout().post()
                                .uri(PAYMENT_CAPTURE)
                                .bodyValue(requestDto)
                                .retrieve()
                                .toEntity(CapturePaymentResponseDto.class)
                                .block(), // synchronously
                externalHttpThreadPool);

        try {
            var captureResponse = future.get();
            log.info("CAPTURE call completed: {}", captureResponse);
            if (captureResponse.getBody() != null) {
                if (captureResponse.getBody().capturedAmount() != null) {
                    processResponse.taskStatus = TaskStatus.SUCCEEDED;
                } else {
                    processResponse.taskStatus = TaskStatus.FAILED_RETRYABLE;
                }
                processResponse.captureResponseDto = captureResponse.getBody();
            }
        } catch (ExecutionException e) {
            log.error("Error while processing task with taskId={}", taskId, e);
            processResponse.taskStatus = TaskStatus.FAILED_NON_RETRYABLE;
            processResponse.message = e.getMessage();
        } catch (InterruptedException e) {
            log.error("Thread was interrupted", e);
            Thread.currentThread().interrupt();
            processResponse.taskStatus = TaskStatus.FAILED_RETRYABLE;
        }
    }

}
