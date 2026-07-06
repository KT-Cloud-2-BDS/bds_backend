package com.bds.chat.presentation.chatRoom.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequestDto(
        @NotBlank @Size(max = 5000) String content,
        String clientId  // 중복 전송 방지용 클라이언트 식별자
) {
}
