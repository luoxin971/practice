package com.luoxin.service;

import java.util.concurrent.Future;

public interface HelloAsyncService {
    Future<String> testAsync(String path) throws InterruptedException;
}
