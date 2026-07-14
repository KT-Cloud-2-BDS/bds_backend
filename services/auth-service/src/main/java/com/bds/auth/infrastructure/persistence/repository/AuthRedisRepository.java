package com.bds.auth.infrastructure.persistence.repository;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthRedisRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX_CODE = "verify:";

    public void saveVerificationCode(String email, String code) {
        String key = KEY_PREFIX_CODE + email;
        redisTemplate.opsForValue().set(key, code, 3, TimeUnit.MINUTES);
    }
}
