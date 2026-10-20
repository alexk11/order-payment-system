package dev.alexk.api;

import dev.alexk.domain.PaymentStatus;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderDto(
        UUID id,
        Long customerId,
        BigDecimal clientEstimate,
        BigDecimal authorizedAmount,
        BigDecimal capturedAmount,
        PaymentStatus paymentStatus,
        String address,
        String description
) { }
