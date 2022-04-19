package com.luoxin.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
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
    @Override
    public String test(String path) throws InterruptedException, ExecutionException {
        Future<String> a1 = helloAsyncService.testAsync(path);
        Future<String> a2 = helloAsyncService.testAsync(StrUtil.reverse(path));
        String s1 = a1.get();
        String s2 = a2.get();
        return s1 + s2;
    }



}
