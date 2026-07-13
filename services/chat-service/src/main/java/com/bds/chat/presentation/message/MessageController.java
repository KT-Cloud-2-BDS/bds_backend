package com.bds.chat.presentation.message;

import com.bds.chat.application.message.dto.MessageDeleteResponseDto;
import com.bds.chat.application.message.dto.MessageListResponseDto;
import com.bds.chat.application.message.service.MessageService;
import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // 채팅 이력 조회
    @GetMapping("/rooms/messages")
    public ResponseEntity<MessageListResponseDto> getHistory(
            @RequestParam(required = false) Long cursor,
            @LoginUser CurrentUser currentUser
    ) {
        return ResponseEntity.ok(messageService.getHistory(currentUser.id(), cursor));
    }

    // 1:1 문의 채팅방 메시지 조회
    @GetMapping("/Inquiries/{roomId}/messages")
    public ResponseEntity<MessageListResponseDto> getInquiryMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @LoginUser CurrentUser currentUser
    ) {
        return ResponseEntity.ok(messageService.getInquiryMessages(roomId, currentUser.id(), cursor));
    }

    // 공개 채팅방 메시지 조회
    @GetMapping("/fundings/{roomId}/messages")
    public ResponseEntity<MessageListResponseDto> getFundingMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @LoginUser CurrentUser currentUser
    ) {
        return ResponseEntity.ok(messageService.getFundingMessages(roomId, cursor));
    }

    // 메시지 삭제
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<MessageDeleteResponseDto> deleteMessage(
            @PathVariable Long messageId,
            @LoginUser CurrentUser currentUser
    ) {
        return ResponseEntity.ok(messageService.delete(messageId, currentUser.id()));
    }
}
