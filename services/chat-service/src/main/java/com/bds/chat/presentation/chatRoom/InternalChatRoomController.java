package com.bds.chat.presentation.chatRoom;

import com.bds.chat.application.chatRoom.dto.ChatRoomResponseDto;
import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/chat")
@RequiredArgsConstructor
public class InternalChatRoomController {

    private final ChatRoomService chatRoomService;

    // 펀딩 제품 생성 시 공개 채팅방 자동 생성
    @PostMapping("/fundings/{productId}")
    public ResponseEntity<ChatRoomResponseDto> createFundingRoom(
            @PathVariable Long productId,
            @RequestBody FundingRoomCreateRequestDto request
    ) {
        ChatRoomResponseDto response = chatRoomService.createFundingRoom(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
