package com.luoxin.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;

/**
 * content
 *
 * @author luoxin
 * @since 2022/4/24
 */
@EnableAsync(proxyTargetClass = true)
@Configuration
@Slf4j
public class RateLimiterConfiguration {
  @Bean("ratelimiter")
  public RateLimiter rateLimiter() {
    // 限制每秒10次调用
    RateLimiterConfig config =
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(1)
            .timeoutDuration(Duration.ofMillis(1))
            .build();
    // Create registry
    RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(config);

    // Use registry
    RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("ratelimiter");
    rateLimiter.getEventPublisher().onFailure(event -> log.error("nice failure"));
    return rateLimiter;
  }
}
