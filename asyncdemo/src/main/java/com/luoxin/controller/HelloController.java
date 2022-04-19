package com.luoxin.controller;

import com.luoxin.aop.SemaphoreLimit;
import com.luoxin.aop.SemaphoreLimitAspect;
import com.luoxin.service.HelloService;
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

  @GetMapping("/test")
  //    @Async("taskExecutor")
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
