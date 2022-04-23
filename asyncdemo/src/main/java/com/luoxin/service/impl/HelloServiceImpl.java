package com.luoxin.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.luoxin.aop.CommonBeanFactory;
import com.luoxin.aop.MetricTime;
import com.luoxin.service.HelloAsyncService;
import com.luoxin.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class HelloServiceImpl implements HelloService {
    @Autowired
    HelloAsyncService helloAsyncService;

  public String asyncTest(String path) throws InterruptedException, ExecutionException {
        Future<String> a1 = helloAsyncService.testAsync(path);
        Future<String> a2 = helloAsyncService.testAsync(StrUtil.reverse(path));
        String s1 = a1.get();
        String s2 = a2.get();
        return s1 + s2;
    }

  @Override
  @MetricTime
  public String test(String path) throws InterruptedException, ExecutionException {
    Future<String> a1 = proxy().testAsyncInClass(path);
    Future<String> a2 = proxy().testAsyncInClass(StrUtil.reverse(path));
    String s1 = a1.get();
    String s2 = a2.get();
    return s1 + s2;
  }

  @Async(value = "taskExecutor")
  @MetricTime
  public Future<String> testAsyncInClass(String path) throws InterruptedException {
    long l = RandomUtil.randomLong(path.length() * 10);
    System.out.println(path + " will run: " + l);
    for (long i = 0; i < l; i++) {
      Thread.sleep(l * 10);
      System.out.println(path + " " + l + " " + i);
    }
    return new AsyncResult<>(NumberUtil.toStr(l));
  }

  public HelloServiceImpl proxy() {
    return CommonBeanFactory.getBean(HelloServiceImpl.class);
  }
}
