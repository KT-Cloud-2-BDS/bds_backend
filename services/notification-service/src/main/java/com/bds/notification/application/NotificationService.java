package com.bds.notification.application;

import com.bds.notification.common.exception.BusinessException;
import com.bds.notification.common.exception.ErrorCode;
import com.bds.notification.infrastructure.sse.SseEmitterManager;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final SseEmitterManager sseEmitterManager;

  // 생성한 SSE Emitter 반환
  public SseEmitter connect(Long memberId) {
    SseEmitter emitter = sseEmitterManager.create(memberId);

    try {
      emitter.send(
          SseEmitter.event()
              .name("connect")
              .data("connected")
      );
    } catch (IOException e) {
      sseEmitterManager.remove(memberId);
      throw new BusinessException(ErrorCode.SSE_SEND_FAILED);
    }

    return emitter;
  }

}
