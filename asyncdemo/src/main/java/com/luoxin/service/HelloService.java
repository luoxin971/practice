package com.luoxin.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface HelloService {
    String test(String path) throws InterruptedException, ExecutionException;

}
