package dev.alexk.async;


import dev.alexk.config.TaskPollerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Component
public class TaskPoller {

    private final TaskEntityRepository taskRepository;
    private final TransactionTemplate txTemplate;
    private final TaskDispatcher taskDispatcher;
    private final TaskPollerProperties taskPollerProperties;


    @Scheduled(fixedDelayString = "${task-execution.poller.poll-interval-ms}")
    public void poll() {
        log.info("Start polling tasks");
        List<TaskEntity> tasksBatch = pickTasksForProcessing();

        var tasksIds = tasksBatch.stream()
                .map(TaskEntity::getId)
                .toList();
        log.info("Successfully picked tasks: count={}, ids={}",
                tasksIds.size(), tasksIds);

        if (tasksBatch.isEmpty()) {
            return;
        }
        tasksBatch.forEach(taskDispatcher::dispatch);
    }


    private List<TaskEntity> pickTasksForProcessing() {
        return txTemplate.execute(status ->  {
            List<TaskEntity> tasks = taskRepository.pickBatchForProcessing(
                    TaskStatus.NEW.getCode(),
                    TaskStatus.FAILED_RETRYABLE.getCode(),
                    TaskStatus.IN_PROGRESS.getCode(),
                    OffsetDateTime.now(),
                    taskPollerProperties.getBatchSize()
            );
            OffsetDateTime nextProcessTime =
                    OffsetDateTime.now().plus(taskPollerProperties.getRetryDelay());
            tasks.forEach(task -> {
                task.setStatus(TaskStatus.IN_PROGRESS);
                task.setInProcessing(true);
                int attempts =
                        task.getAttempts() == null ? 1 : task.getAttempts() + 1;
                task.setAttempts(attempts);
                task.setNextAttemptAt(nextProcessTime);
            });
            taskRepository.saveAll(tasks);
            return tasks;
        });
    }

}
