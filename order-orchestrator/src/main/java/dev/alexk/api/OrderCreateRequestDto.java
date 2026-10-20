package dev.alexk.api;

import java.math.BigDecimal;

public record OrderCreateRequestDto(
        Long customerId,
        BigDecimal amount,
        String address,
        String description
) { }
