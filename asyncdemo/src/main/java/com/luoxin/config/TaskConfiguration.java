package com.luoxin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync(proxyTargetClass = true)
@Configuration
public class TaskConfiguration {

  @Bean("taskExecutor")
  public Executor initExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(9);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setKeepAliveSeconds(60);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAllowCoreThreadTimeOut(true);
    executor.setAwaitTerminationSeconds(60);
    executor.setThreadNamePrefix("taskExecutor-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    //        executor.setTaskDecorator(new ContextCopyingDecorator());
    return executor;
  }
}
