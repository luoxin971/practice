package com.luoxin.service;

import java.util.concurrent.ExecutionException;

public interface HelloService {
  String test(String path) throws InterruptedException, ExecutionException;

  String ggg(String path);

  String xxx(String path);
}
