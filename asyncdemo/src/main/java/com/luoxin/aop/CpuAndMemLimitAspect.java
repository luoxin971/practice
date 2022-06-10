package com.luoxin.aop;

import com.sun.management.OperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;

@Component
@Scope
@Aspect
@Order(1)
@Slf4j
public class CpuAndMemLimitAspect {

  /** 业务层切点 */
  @Pointcut("@annotation(com.luoxin.aop.CpuAndMemLimit)")
  public void ServiceAspect() {}

  @Around("ServiceAspect()")
  public Object around(ProceedingJoinPoint joinPoint) {
    // 获取增强方法信息
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    CpuAndMemLimit annotation = signature.getMethod().getAnnotation(CpuAndMemLimit.class);
    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    double processCpuLoad = osBean.getSystemCpuLoad();
    double memLoad =
        1 - (double) osBean.getFreePhysicalMemorySize() / osBean.getTotalPhysicalMemorySize();
    log.info("cpu usage: {}", processCpuLoad);
    log.info("mem usage: {}", memLoad);
    if (memLoad > annotation.memRate() || processCpuLoad > annotation.cpuRate()) {
      throw new RuntimeException("busy");
    }
    try {
      return joinPoint.proceed();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return null;
  }
}
