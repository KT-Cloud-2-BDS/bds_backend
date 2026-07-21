package com.bds.chat.application.chatRoom.dto;

import java.util.List;

public record InquiryRoomListResponseDto(
        List<InquiryRoomSummaryDto> rooms,
        Long nextCursor,
        Boolean hasNext,
        int totalCount
) {
    public static InquiryRoomListResponseDto empty() {
        return new InquiryRoomListResponseDto(List.of(), null, false, 0);
    }
}
