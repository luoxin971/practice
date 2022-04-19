package com.luoxin.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.luoxin.service.HelloAsyncService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

@Service
public class HelloAsyncServiceImpl implements HelloAsyncService {
  @Async(value = "taskExecutor")
  @Override
  public Future<String> testAsync(String path) throws InterruptedException {
    long l = RandomUtil.randomLong(path.length() * 10);
    System.out.println(path + " will run: " + l);
    for (long i = 0; i < l; i++) {
      Thread.sleep(l * 10);
      System.out.println(path + " " + l + " " + i);
    }
    return new AsyncResult<>(NumberUtil.toStr(l));
  }
}
