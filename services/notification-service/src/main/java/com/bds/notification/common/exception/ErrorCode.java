package com.bds.notification.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // SSE
  SSE_SEND_FAILED(500, "SSE 이벤트 전송에 실패했습니다."),

  // 구독
  SUBSCRIPTION_ALREADY_EXISTS(409, "이미 구독 중인 알림입니다."),
  SUBSCRIPTION_NOT_FOUND(404, "구독 정보를 찾을 수 없습니다."),
  INVALID_TARGET_TYPE(400, "유효하지 않은 구독 타입입니다."),

  // FCM
  FCM_TOKEN_NOT_FOUND(404, "FCM 토큰을 찾을 수 없습니다.");

  private final int status;
  private final String message;
}
