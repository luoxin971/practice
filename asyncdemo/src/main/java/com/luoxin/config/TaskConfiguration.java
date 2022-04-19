package com.luoxin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class TaskConfiguration {
    @Bean("taskExecutor")
    public Executor schedulingTaskExecutor() {
        return initExecutor(4, 8, 100, new ThreadPoolExecutor.AbortPolicy(), "taskExecutor-");
    }

    private Executor initExecutor(int corePoolSize, int maxPoolSize, int queueCapacity, RejectedExecutionHandler rejectedExecutionHandler, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);
//        executor.setTaskDecorator(new ContextCopyingDecorator());
        return executor;
    }
}
