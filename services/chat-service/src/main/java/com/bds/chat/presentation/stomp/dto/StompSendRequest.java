package com.bds.chat.presentation.stomp.dto;

public record StompSendRequest(String clientMessageId, Long roomId, String type, String content) {}
