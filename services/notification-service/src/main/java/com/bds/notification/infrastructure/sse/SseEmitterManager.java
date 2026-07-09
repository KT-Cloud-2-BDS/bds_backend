package com.bds.notification.infrastructure.sse;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterManager {

  private static final long SSE_TIMEOUT = 30 * 60 * 1000L;
  private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

  // 1. Emitter 생성
  public SseEmitter create(Long memberId) {
    SseEmitter sseEmitter = new SseEmitter(SSE_TIMEOUT);

    SseEmitter existing = emitters.get(memberId);
    if (existing != null) {
      existing.complete();
    }

    sseEmitter.onCompletion(() -> emitters.remove(memberId, sseEmitter));
    sseEmitter.onTimeout(() -> emitters.remove(memberId, sseEmitter));
    sseEmitter.onError(e -> emitters.remove(memberId, sseEmitter));

    emitters.put(memberId, sseEmitter);

    return sseEmitter;
  }

  //2. HashMap에서 member 찾기
  public Optional<SseEmitter> findByMemberId(Long memberId) {
    return Optional.ofNullable(emitters.get(memberId));

  }

  public void remove(Long memberId) {
    emitters.remove(memberId);
  }
}
