package dev.alexk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "task-execution.dispatcher")
public class TaskDispatcherProperties {

    // задержка перед повторной попыткой
    private Duration retryDelay;

    // размер thread pool dispatcher
    private int threadPoolSize;

    // максимальное количество попыток выполнения
    private int maxAttempts;
}
