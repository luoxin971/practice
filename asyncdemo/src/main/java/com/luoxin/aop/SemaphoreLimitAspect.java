package com.luoxin.aop;

import com.sun.management.OperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Component
@Scope
@Aspect
@Slf4j
public class SemaphoreLimitAspect {

  /** 存储限流量和方法,必须是static且线程安全,保证所有线程进入都唯一 */
  public static Map<String, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

  /** 业务层切点 */
  @Pointcut("@annotation(com.luoxin.aop.SemaphoreLimit)")
  public void ServiceAspect() {}

  @Around("ServiceAspect()")
  public Object around(ProceedingJoinPoint joinPoint) {
    // 获取目标对象
    Class<?> clz = joinPoint.getTarget().getClass();
    // 获取增强方法信息
    Signature signature = joinPoint.getSignature();
    String name = signature.getName();
    String limitKey = getLimitKey(clz, name);
    Semaphore semaphore = semaphoreMap.get(limitKey);
    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    double processCpuLoad = osBean.getSystemCpuLoad();
    double memLoad =
        1 - (double) osBean.getFreePhysicalMemorySize() / osBean.getTotalPhysicalMemorySize();
    log.info("cpu usage: {}%", processCpuLoad);
    log.info("mem usage: {}%", memLoad);
    if (memLoad > 0.75) {
      throw new RuntimeException("busy");
    }
    // 立即获取许可证,非阻塞
    boolean flag = semaphore.tryAcquire();
    Object obj = null;
    try {
      // 拿到许可证则执行任务
      if (flag) {
        log.info("succeed!");
        obj = joinPoint.proceed();
      } else {
        // 拒绝了请求（服务降级）
        obj = "limitKey:" + limitKey + ", The system is busy, please visit after a while";
        log.error("限流！ available permits: {}", semaphore.availablePermits());
      }
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      if (flag) {
        semaphore.release();
        log.info("release!");
      } // 拿到许可证后释放通行证
    }
    return obj;
  }

  /** 获取拦截方法配置的限流key,没有返回null */
  private String getLimitKey(Class<?> clz, String methodName) {
    for (Method method : clz.getDeclaredMethods()) {
      // 找出目标方法
      if (method.getName().equals(methodName)) {
        // 判断是否是限流方法
        if (method.isAnnotationPresent(SemaphoreLimit.class)) {
          return method.getAnnotation(SemaphoreLimit.class).limitKey();
        }
      }
    }
    return null;
  }
}
