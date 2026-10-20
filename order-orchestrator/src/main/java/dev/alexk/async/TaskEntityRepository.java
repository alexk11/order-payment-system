package dev.alexk.async;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface TaskEntityRepository extends JpaRepository<TaskEntity, Long> {

    @Query(value = """
                select * from tasks
                where in_processing = false
                and ((status = :newStatus) 
                    or (status = :retryStatus and next_attempt_at <= :now)
                    or (status = :progressStatus and next_attempt_at <= :now))
                order by id
                limit :batchSize
                for update skip locked
            """, nativeQuery = true)
    List<TaskEntity> pickBatchForProcessing(
            @Param("newStatus") int newStatus,
            @Param("retryStatus") int retryStatus,
            @Param("progressStatus") int progressStatus,
            @Param("now") OffsetDateTime now,
            @Param("batchSize") int batchSize
    );

}
