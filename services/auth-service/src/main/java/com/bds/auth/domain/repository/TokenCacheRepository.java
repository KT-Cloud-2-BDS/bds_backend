package com.bds.auth.domain.repository;

import java.util.concurrent.TimeUnit;

public interface TokenCacheRepository {
    void put(String key, String value, long timeoutMinutes);
    String get(String key);
    void delete(String key);
    void save(String key, String value, long timeout, TimeUnit unit);
}