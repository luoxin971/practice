package com.luoxin;

import com.luoxin.service.HelloService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.function.Supplier;

@SpringBootTest
class DemoApplicationTests {

  @Autowired HelloService helloService;

  @Test
  void aaa() throws InterruptedException {
    // 为断路器创建自定义的配置
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
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("name");

    Supplier<String> decoratedSupplier =
        CircuitBreaker.decorateSupplier(circuitBreaker, () -> helloService.ggg("a"));

    while (true) {
      // String result =
      //     Try.ofSupplier(decoratedSupplier).recover(throwable -> "Hello from Recovery").get();
      String result = "";
      try {

        result = circuitBreaker.executeSupplier(decoratedSupplier);
      } catch (RuntimeException e) {
        System.out.println("aaa" + e.getMessage());
        Thread.sleep(200);
      }
      System.out.println(result);
    }
  }

  @Test
  void bbb() throws InterruptedException {
    RateLimiterConfig config =
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofMillis(25))
            .build();

    // 创建Registry
    RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(config);

    // 使用registry
    RateLimiter rateLimiterWithDefaultConfig = rateLimiterRegistry.rateLimiter("name1");

    RateLimiter rateLimiterWithCustomConfig = rateLimiterRegistry.rateLimiter("name2", config);

    CheckedRunnable restrictedCall =
        RateLimiter.decorateCheckedRunnable(
            rateLimiterWithCustomConfig, () -> helloService.ggg("a"));
    while (true) {
      String result = "";
      try {
        Try<Void> voids =
            Try.run(restrictedCall).onFailure(e -> System.out.println(e.getMessage()));
      } catch (RuntimeException e) {
        System.out.println("aaa" + e.getMessage());
        // Thread.sleep(200);
      }
      System.out.println(result);
    }
  }
}
