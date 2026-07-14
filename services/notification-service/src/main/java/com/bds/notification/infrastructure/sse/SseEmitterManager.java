package com.bds.notification.infrastructure.sse;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@EnableAsync
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

  // SSE Emitter Send 로직
  @Async
  public void send(Long memberId, String eventName, Object data) {
    SseEmitter sseEmitter = emitters.get(memberId);
    if (sseEmitter == null) {
      return;
    }

    try {
      sseEmitter.send(
          SseEmitter.event()
              .name(eventName)
              .data(data) // TODO: String으로 넣을지 고민해봐야함.또는 data안에 title이랑 body를 넣는다면 이거 객체를 만들어야할듯
      );
    } catch (IOException e) {
      emitters.remove(memberId, sseEmitter);
    }
  }

  public void remove(Long memberId) {
    emitters.remove(memberId);
  }
}
