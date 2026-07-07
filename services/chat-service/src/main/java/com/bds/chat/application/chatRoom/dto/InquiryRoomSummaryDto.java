package com.bds.chat.application.chatRoom.dto;

import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.message.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public record InquiryRoomSummaryDto(
        Long roomId,
        String type,
        Long productId,
        List<Long> participants,
        Long createdBy,
        LocalDateTime createdAt,
        LastMessageDto lastMessage,
        long unreadCount,
        String status
) {
    public static InquiryRoomSummaryDto from(ChatRoom room,
                                              List<Long> participants,
                                              ChatMessage lastMessage,
                                              long unreadCount) {
        return new InquiryRoomSummaryDto(
                room.getId().value(),
                room.getType().name(),
                room.getProductId().value(),
                participants,
                room.getCreatorId().value(),
                room.getCreatedAt(),
                lastMessage != null ? LastMessageDto.from(lastMessage) : null,
                unreadCount,
                room.getStatus().name()
        );
    }
}
