package com.luoxin.aop;

import com.google.common.util.concurrent.RateLimiter;
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

@Component
@Scope
@Aspect
@Slf4j
@Order(2)
@SuppressWarnings("UnstableApiUsage")
public class TrafficLimitAspect {

  public static Map<String, RateLimiter> trafficLimitMap = new ConcurrentHashMap<>();

  /** 业务层切点 */
  @Pointcut("@annotation(com.luoxin.aop.TrafficLimit)")
  public void ServiceAspect() {}

  @Around("ServiceAspect()")
  public Object around(ProceedingJoinPoint joinPoint) {

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    TrafficLimit annotation = signature.getMethod().getAnnotation(TrafficLimit.class);
    RateLimiter rateLimiter = trafficLimitMap.get(annotation.limitKey());
    Object obj = null;
    try {
      // 拿到许可证则执行任务
      if (rateLimiter.tryAcquire()) {
        log.info("succeed! {}", rateLimiter.getRate());
        obj = joinPoint.proceed();
      } else {
        // 拒绝了请求（服务降级）
        obj = "系统繁忙，清稍后重试";
        log.error(obj.toString());
        // throw new RuntimeException();
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return obj;
  }
}
