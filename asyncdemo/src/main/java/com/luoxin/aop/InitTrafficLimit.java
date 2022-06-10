package com.luoxin.aop;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * content
 *
 * @author luoxin
 * @since 2022/4/21
 */
@Component
public class InitTrafficLimit implements ApplicationContextAware {

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(RestController.class);
    beanMap.forEach(
        (k, v) -> {
          Class<?> controllerClass = v.getClass();
          System.out.println(controllerClass.toString());
          System.out.println(controllerClass.getSuperclass().toString());
          // 获取所有声明的方法
          Method[] allMethods = controllerClass.getSuperclass().getDeclaredMethods();
          for (Method method : allMethods) {
            System.out.println(method.getName());
            // 判断方法是否使用了限流注解
            if (method.isAnnotationPresent(TrafficLimit.class)) {
              // 获取配置的限流量,实际值可以动态获取,配置key,根据key从配置文件获取
              double value = method.getAnnotation(TrafficLimit.class).value();
              String key = method.getAnnotation(TrafficLimit.class).limitKey();
              System.out.println("limitKey:" + key + ",许可证数是" + value);
              // key作为key.value为具体限流量,传递到切面的map中
              TrafficLimitAspect.trafficLimitMap.put(key, RateLimiter.create(value));
            }
          }
        });
  }
}
