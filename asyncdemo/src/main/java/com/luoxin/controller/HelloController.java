package com.luoxin.controller;

import com.luoxin.aop.CpuAndMemLimit;
import com.luoxin.aop.SemaphoreLimit;
import com.luoxin.aop.SemaphoreLimitAspect;
import com.luoxin.aop.TrafficLimit;
import com.luoxin.service.HelloService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;

@RestController
@Slf4j
public class HelloController {

  @Resource HelloService helloService;

  @Resource ThreadPoolTaskExecutor taskExecutor;

  @Resource io.github.resilience4j.circuitbreaker.CircuitBreaker first;
  @Resource io.github.resilience4j.ratelimiter.RateLimiter rateLimiter;

  @GetMapping("/aaa")
  public String aaa() {
    return "great";
  }

  @GetMapping("/bbb")
  @SemaphoreLimit(limitKey = "SemaphoreKey", value = 30)
  public String bbb() {
    return "bbb";
  }

  @GetMapping("/ccc")
  @TrafficLimit(limitKey = "ccc", value = 3)
  public String ccc() {
    return "ccc";
  }

  @GetMapping("/ddd")
  @CpuAndMemLimit(cpuRate = 0.8, memRate = 0.8)
  public String ddd() {
    return "ddd";
  }

  @GetMapping("/eee")
  @CpuAndMemLimit(cpuRate = 0.8, memRate = 0.8)
  @TrafficLimit(limitKey = "eee", value = 2)
  public String eee() {
    return "ddd";
  }

  @GetMapping("/fff")
  @CpuAndMemLimit(cpuRate = 0.8, memRate = 1)
  @TrafficLimit(limitKey = "fff", value = 2)
  public String fff() {
    return "fff";
  }

  @GetMapping("/ggg")
  @RateLimiter(name = "ratelimiter", fallbackMethod = "gggFallback")
  public String ggg(String path) throws InterruptedException {
    // Thread.sleep(5000);
    return helloService.ggg(path);
  }

  @GetMapping("/abc")
  public String abc(String path) {
    log.warn("circuitBreaker status: {}", first.getState());
    return helloService.ggg(path);
  }

  @GetMapping("/xxx")
  public String xxx(String path) {
    try {
      return helloService.xxx(path);
    } catch (CallNotPermittedException e) {
      log.error("被限流了");
    } catch (RuntimeException e) {
      log.error(e.getMessage());
    }
    return "final";
  }

  @GetMapping("yyy")
  @Bulkhead(name = "bulkhead", fallbackMethod = "gggFallback")
  @RateLimiter(name = "ratelimiter")
  public String yyy(String path) {
    log.info("yes" + path);
    return "yyy" + path;
  }

  @GetMapping("zzz")
  public String zzz(String path) throws Exception {
    System.out.println(rateLimiter.getMetrics());
    String s = rateLimiter.executeCallable(() -> helloService.ggg(path));
    log.info("yes" + path);
    return "yyy" + path;
  }

  @GetMapping("xyz")
  @RateLimiter(name = "ratelimiter")
  public String xyz(String path) {
    log.info(path + "进来了" + rateLimiter.getMetrics().getAvailablePermissions());
    helloService.ggg(path);
    log.info("yes" + path + rateLimiter.getMetrics().getAvailablePermissions());
    return "yyy" + path;
  }

  public String gggFallback(String a, Throwable e) {
    log.error("降级了dsafdsfdas");
    return "ggg fallback";
  }

  @GetMapping("/test")
  @SemaphoreLimit(limitKey = "SemaphoreKey", value = 30)
  public String test(@RequestParam("path") String path) {
    try {
      log.info(
          "限流！ available permits: {}",
          SemaphoreLimitAspect.semaphoreMap.get("SemaphoreKey").availablePermits());
      return helloService.test(path);
    } catch (InterruptedException e) {
      log.error(e.getMessage());
      return e.getMessage();
    } catch (ExecutionException e) {
      log.error(e.getMessage());
      return e.getMessage();
    } catch (Exception e) {
      log.error(e.getMessage());
      return e.getMessage();
    }
  }

  @GetMapping("/status")
  public String status() {
    String statusStr = "";
    int queueSize = taskExecutor.getThreadPoolExecutor().getQueue().size();
    statusStr += "当前排队线程数：" + queueSize;
    int activeCount = taskExecutor.getThreadPoolExecutor().getActiveCount();
    statusStr += "当前活动线程数：" + activeCount;
    long completedTaskCount = taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
    statusStr += "执行完成线程数：" + completedTaskCount;
    long taskCount = taskExecutor.getThreadPoolExecutor().getTaskCount();
    statusStr += "总线程数：" + taskCount;
    return statusStr;
  }

  @GetMapping("/limitStatus")
  public String limitStatus() {
    return String.format(
        "Available permits: %s",
        SemaphoreLimitAspect.semaphoreMap.get("SemaphoreKey").availablePermits());
  }
}
