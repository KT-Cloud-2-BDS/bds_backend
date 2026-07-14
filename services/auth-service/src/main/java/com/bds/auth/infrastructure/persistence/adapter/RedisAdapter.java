package com.bds.auth.infrastructure.persistence.adapter;

import com.bds.auth.domain.repository.TokenCacheRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisAdapter implements TokenCacheRepository {
    private final StringRedisTemplate redisTemplate;

    /**
     * Redis에 키-값 저장 및 만료 시간 설정
     */
    @Override
    public void put(String key, String value, long timeoutInMinutes) {
        redisTemplate.opsForValue().set(key, value, timeoutInMinutes, TimeUnit.MINUTES);
    }

    /**
     * Redis에서 키로 값 조회
     */
    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 사용이 완료된 인증번호 키 삭제
     */
    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * RefreshToken 저장용 유연한 메서드
     */
    @Override
    public void save(String key, String value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }
}
