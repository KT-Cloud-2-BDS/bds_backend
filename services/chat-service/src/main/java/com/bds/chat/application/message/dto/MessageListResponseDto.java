package com.bds.chat.application.message.dto;

import java.util.List;

public record MessageListResponseDto(
        List<MessageResponseDto> messages,
        Long nextCursor,
        boolean hasNext,
        int totalCount
) {}
