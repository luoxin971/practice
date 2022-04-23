package com.luoxin.aop;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

@Component
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@Aspect
@Configuration
@Slf4j
public class MetricTimeAspect {
  @Pointcut("@annotation(com.luoxin.aop.MetricTime)")
  public void metricPointcut() {}

  @Around("metricPointcut()")
  public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      return joinPoint.proceed();
    } finally {
      log.info(
          "{}.{} cost: {}",
          joinPoint.getSignature().getDeclaringTypeName(),
          joinPoint.getSignature().getName(),
          stopwatch.stop());
    }
  }
}
