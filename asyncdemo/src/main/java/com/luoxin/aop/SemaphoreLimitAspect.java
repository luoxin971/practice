package com.luoxin.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Component
@Scope
@Aspect
@Order(1)
@Slf4j
public class SemaphoreLimitAspect {

  /** 存储限流量和方法,必须是static且线程安全,保证所有线程进入都唯一 */
  public static Map<String, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

  /** 业务层切点 */
  @Pointcut("@annotation(com.luoxin.aop.SemaphoreLimit)")
  public void ServiceAspect() {}

  @Around("ServiceAspect()")
  public Object around(ProceedingJoinPoint joinPoint) {

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    TrafficLimit annotation = signature.getMethod().getAnnotation(TrafficLimit.class);
    Semaphore semaphore = semaphoreMap.get(annotation.limitKey());
    // 立即获取许可证,非阻塞
    boolean flag = semaphore.tryAcquire();
    Object obj = null;
    try {
      if (flag) {
        // 拿到许可证则执行任务
        obj = joinPoint.proceed();
      } else {
        // 拒绝了请求（服务降级）
        obj = "系统繁忙，清稍后重试";
        log.error(obj.toString());
      }
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      if (flag) {
        // 拿到许可证后释放通行证
        semaphore.release();
      }
    }
    return obj;
  }
}
