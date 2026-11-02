package dev.alexk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Configuration
public class ExecutorServiceConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService taskDispatcherThreadPool(TaskDispatcherProperties properties) {
        return Executors.newFixedThreadPool(properties.getThreadPoolSize());
//        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService externalHttpThreadPool(
            @Value("${task-execution.external-http.thread-pool-size}") int threadPoolSize) {
//        return Executors.newFixedThreadPool(threadPoolSize);
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

