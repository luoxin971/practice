package com.luoxin;

import com.luoxin.service.HelloService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
}
