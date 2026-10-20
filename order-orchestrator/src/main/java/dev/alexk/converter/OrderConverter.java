package dev.alexk.converter;


import dev.alexk.api.OrderCreateRequestDto;
import dev.alexk.api.OrderDto;
import dev.alexk.domain.OrderEntity;
import org.springframework.stereotype.Component;


@Component
public class OrderConverter {

    public OrderEntity toOrderEntity(OrderCreateRequestDto dto) {
        return OrderEntity.builder()
                .customerId(dto.customerId())
                .clientEstimate(dto.amount())
                .address(dto.address())
                .description(dto.description())
                .build();
    }

    public OrderDto toOrderDto(OrderEntity entity) {
        return OrderDto.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .clientEstimate(entity.getClientEstimate())
                .authorizedAmount(entity.getAuthorizedAmount())
                .capturedAmount(entity.getCapturedAmount())
                .paymentStatus(entity.getPaymentStatus())
                .address(entity.getAddress())
                .description(entity.getDescription())
                .build();
    }
}
