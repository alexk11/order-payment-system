package dev.alexk.async;


import dev.alexk.converter.TaskStatusConverter;
import dev.alexk.domain.OrderEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private OrderEntity order;

    @Column(name = "status", nullable = false)
    @Convert(converter = TaskStatusConverter.class)
    private TaskStatus status;

    @Column(name="inProcessing")
    private Boolean inProcessing;

    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        var now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
