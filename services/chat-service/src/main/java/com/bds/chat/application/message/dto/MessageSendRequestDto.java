package com.bds.chat.application.message.dto;

public record MessageSendRequestDto(Long roomId, String content, String type, String clientId) {}
