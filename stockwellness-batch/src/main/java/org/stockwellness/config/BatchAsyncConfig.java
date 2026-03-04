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
        executor.setCorePoolSize(10);      // 기본 10개 스레드 유지
        executor.setMaxPoolSize(20);      // 최대 20개까지 확장
        executor.setQueueCapacity(200);   // 대기 큐 확장
        executor.setThreadNamePrefix("Batch-Common-");
        executor.initialize();
        return executor;
    }
}
