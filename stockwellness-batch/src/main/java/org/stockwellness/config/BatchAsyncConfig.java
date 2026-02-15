package org.stockwellness.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class BatchAsyncConfig {

    @Bean(name = "batchExecutor")
    public TaskExecutor batchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // 기본 스레드 수
        executor.setMaxPoolSize(10);      // 최대 스레드 수
        executor.setQueueCapacity(100);   // 대기 큐 크기
        executor.setThreadNamePrefix("Batch-Async-");
        executor.initialize();
        return executor;
    }
}