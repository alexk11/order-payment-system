package dev.alexk.async;


import dev.alexk.payment.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import dev.alexk.config.PaymentWebClient;
import dev.alexk.warehouse.CalculatePricingRequestDto;
import dev.alexk.warehouse.CalculatePricingResponseDto;


@Slf4j
@Component
@AllArgsConstructor
public class TaskProcessor {

    private final PaymentWebClient webClient;

    private static final String PAYMENT_AUTHORIZE = "/payment/authorize";
    private static final String PAYMENT_CAPTURE = "/payment/capture";
    private static final String WAREHOUSE = "/warehouse/calculate-price";

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
        var authResponse = webClient.webClientWithTimeout().post()
            .uri(PAYMENT_AUTHORIZE)
            .bodyValue(requestDto)
            .retrieve()
            .toEntity(AuthorizePaymentResponseDto.class)
            .block();

        if (authResponse != null) {
            log.info("AUTH call completed with result {}", authResponse);
            if (authResponse.getBody() != null) {
                if (authResponse.getBody().authorizationId() != null) {
                    processResponse.taskStatus = TaskStatus.SUCCEEDED;
                }
                else if (authResponse.getBody().status() == AuthorizationStatus.DECLINED) {
                    processResponse.taskStatus = TaskStatus.FAILED_NON_RETRYABLE;
                }
                processResponse.authResponseDto = authResponse.getBody();
            } else { // connection refused (?)
                processResponse.taskStatus = TaskStatus.FAILED_RETRYABLE;
            }
        }
    }


    private void callWarehouseStub(
            Long taskId,
            CalculatePricingRequestDto requestDto,
            TaskProcessResponse processResponse) {
        log.info("Calling warehouse stub for taskId={}", taskId);
        var repriceResponse = webClient.webClientWithTimeout().post()
            .uri(WAREHOUSE)
            .bodyValue(requestDto)
            .retrieve()
            .toEntity(CalculatePricingResponseDto.class)
            .block();

        if (repriceResponse != null) {
            log.info("REPRICE call completed: {}", repriceResponse);
            if (repriceResponse.getBody() != null) {
                if (repriceResponse.getBody().finalAmount() != null) {
                    processResponse.taskStatus = TaskStatus.SUCCEEDED;
                } else {
                    processResponse.taskStatus = TaskStatus.FAILED_RETRYABLE;
                }
                processResponse.pricingResponseDto = repriceResponse.getBody();
            } else {
                processResponse.taskStatus = TaskStatus.FAILED_RETRYABLE;
            }
        }
    }


    private void callPaymentCaptureStub(
            Long taskId,
            CapturePaymentRequestDto requestDto,
            TaskProcessResponse processResponse) {
        log.info("Calling capture stub for taskId={}", taskId);
        var captureResponse = webClient.webClientWithTimeout().post()
            .uri(PAYMENT_CAPTURE)
            .bodyValue(requestDto)
            .retrieve()
            .toEntity(CapturePaymentResponseDto.class)
            .block();

        if (captureResponse != null) {
            log.info("CAPTURE call completed: {}", captureResponse);
            if (captureResponse.getBody() != null) {
                if (captureResponse.getBody().capturedAmount() != null) {
                    processResponse.taskStatus = TaskStatus.SUCCEEDED;
                } else {
                    processResponse.taskStatus = TaskStatus.FAILED_RETRYABLE;
                }
                processResponse.captureResponseDto = captureResponse.getBody();
            } else {
                processResponse.taskStatus = TaskStatus.FAILED_RETRYABLE;
            }
        }
    }

}
