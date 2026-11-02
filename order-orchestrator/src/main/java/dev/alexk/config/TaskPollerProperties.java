package dev.alexk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "task-execution.poller")
public class TaskPollerProperties {

    // сколько брать задач за раз
    private int batchSize;

    // задержка перед повторной вычиткой таски из БД
    private Duration retryDelay;
}
