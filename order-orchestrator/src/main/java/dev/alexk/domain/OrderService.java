package dev.alexk.domain;


import dev.alexk.api.OrderCreateRequestDto;
import dev.alexk.async.TaskEntity;
import dev.alexk.async.TaskEntityRepository;
import dev.alexk.async.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TaskEntityRepository taskRepository;
    private final TransactionTemplate txTemplate;

    public OrderEntity createOrder(
            OrderCreateRequestDto requestDto
    ) {
        log.info("Creating order {}", requestDto);
        var entity = OrderEntity.builder()
                .customerId(requestDto.customerId())
                .clientEstimate(requestDto.amount())
                .paymentStatus(PaymentStatus.NEW)
                .address(requestDto.address())
                .description(requestDto.description())
                .build();

        return txTemplate.execute(status -> {
            var created = orderRepository.save(entity);
            log.info("Created order id={}", created.getId());

            var task = TaskEntity.builder()
                    .order(created)
                    .status(TaskStatus.NEW)
                    .inProcessing(false)
                    .attempts(0)
                    .build();

            var savedTask = taskRepository.save(task);
            log.info("Created task for order creation: taskId={}, orderId={}",
                    savedTask.getId(), created.getId());
            return created;
        });
    }

    public Optional<OrderEntity> findOrder(UUID id) {
        return orderRepository.findById(id);
    }
}
