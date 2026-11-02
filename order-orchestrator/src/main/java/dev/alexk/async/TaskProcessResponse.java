package dev.alexk.async;


import dev.alexk.payment.AuthorizePaymentResponseDto;
import dev.alexk.payment.CapturePaymentResponseDto;
import dev.alexk.warehouse.CalculatePricingResponseDto;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@RequiredArgsConstructor
@Setter
public class TaskProcessResponse {
    TaskStatus taskStatus;
    AuthorizePaymentResponseDto authResponseDto;
    CalculatePricingResponseDto pricingResponseDto;
    CapturePaymentResponseDto captureResponseDto;
    String message;
}
