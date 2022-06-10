package com.luoxin.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;

@EnableAsync(proxyTargetClass = true)
@Configuration
public class CircuitBreakerConfiguration {

  @Bean("first")
  public CircuitBreaker ofDefaults() {
    CircuitBreakerConfig circuitBreakerConfig =
        CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(2)
            .permittedNumberOfCallsInHalfOpenState(2)
            .failureRateThreshold(10)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .recordExceptions(Exception.class)
            .build();

    // 使用自定义的全局配置创建CircuitBreakerRegistry
    CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("first");
    return circuitBreaker;
  }

  @Bean("bulkhead")
  public Bulkhead bulkhead() {
    BulkheadConfig config =
        BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitDuration(Duration.ZERO).build();

    BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(config);
    // bulkheadRegistry.getEventPublisher().onen

    Bulkhead bulkhead = bulkheadRegistry.bulkhead("bulkhead");
    return bulkhead;
  }
}
