package com.bds.chat.application.chatRoom.dto;

import com.bds.chat.domain.chatRoom.ChatRoom;

import java.time.LocalDateTime;
import java.util.List;

public record InquiryChatRoomDetailResponseDto(
        Long roomId,
        String type,
        Long productId,
        List<Long> participants,
        Long createdBy,
        LocalDateTime createdAt,
        String status,
        MembershipStatusDto myMembership,
        LastMessageDto lastMessage
) {
    public static InquiryChatRoomDetailResponseDto from(ChatRoom room, List<Long> participants,
                                                        MembershipStatusDto myMembership, LastMessageDto lastMessage) {
        return new InquiryChatRoomDetailResponseDto(
                room.getId().value(),
                room.getType().name(),
                room.getProductId().value(),
                participants,
                room.getCreatorId().value(),
                room.getCreatedAt(),
                room.getStatus().name(),
                myMembership,
                lastMessage
        );
    }
}
