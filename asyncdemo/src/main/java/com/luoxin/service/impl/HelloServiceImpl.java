package com.luoxin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.luoxin.service.HelloAsyncService;
import com.luoxin.service.HelloService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
@Slf4j
public class HelloServiceImpl implements HelloService {
  @Autowired HelloAsyncService helloAsyncService;

  @Override
  public String test(String path) throws InterruptedException, ExecutionException {
    Future<String> a1 = helloAsyncService.testAsync(path);
    Future<String> a2 = helloAsyncService.testAsync(StrUtil.reverse(path));
    String s1 = a1.get();
    String s2 = a2.get();
    return s1 + s2;
  }

  @Override
  public String ggg(String path) {

    if ("error".equals(path)) {
      log.error("failed req");
      throw new RuntimeException("error");
    }
    log.info("success");
    return "ggg";
  }

  @Override
  @CircuitBreaker(name = "meta")
  public String xxx(String path) {
    if ("error".equals(path)) {
      log.error("failed req");
      throw new RuntimeException("error");
    }
    log.info("success");
    return "xxx";
  }

  private String metaFallBack(String path, Throwable throwable) {
    log.info(throwable.getMessage() + ", 降级");
    // CircuitBreakerUtil.getCircuitBreakerStatus(
    //     "降级方法中:", circuitBreakerRegistry.circuitBreaker("backendA"));
    return "降级";
  }
}
