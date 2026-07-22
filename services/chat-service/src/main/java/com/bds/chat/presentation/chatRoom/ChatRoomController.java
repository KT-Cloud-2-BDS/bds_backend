package com.bds.chat.presentation.chatRoom;

import com.bds.chat.application.chatRoom.dto.*;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.application.member.dto.InquiryMemberLeaveResponseDto;
import com.bds.chat.application.member.service.InquiryRoomMemberService;
import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final InquiryRoomMemberService inquiryRoomMemberService;

    // 1:1 문의 채팅방 생성
    @PostMapping("/Inquiries")
    public ResponseEntity<ChatRoomResponseDto> createInquiryRoom(
            @RequestBody ChatRoomCreateRequestDto request,
            @LoginUser CurrentUser currentUser
    ) {
        ChatRoomResponseDto response = chatRoomService.createInquiryRoom(request.productId(), currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 내 참여 문의 채팅방 목록 조회
    @GetMapping("/Inquiries")
    public ResponseEntity<InquiryRoomListResponseDto> getMyInquiryRooms(
            @RequestParam(required = false) Long cursor,
            @LoginUser CurrentUser currentUser
    ) {
        InquiryRoomListResponseDto response = chatRoomService.getMyInquiryRooms(currentUser.id(), cursor, 20);
        return ResponseEntity.ok(response);
    }

    // 1:1 문의 채팅방 상세 조회
    @GetMapping("/Inquiries/{roomId}")
    public ResponseEntity<InquiryChatRoomDetailResponseDto> getInquiryRoom(
            @PathVariable Long roomId,
            @LoginUser CurrentUser currentUser
    ) {
        InquiryChatRoomDetailResponseDto response = chatRoomService.getInquiryChatRoomById(roomId, currentUser.id());
        return ResponseEntity.ok(response);
    }

    // 공개 채팅방 삭제
    @DeleteMapping("/rooms/{roomId}/close")
    public ResponseEntity<ChatRoomDeleteResponseDto> closeRoom(
            @PathVariable Long roomId,
            @LoginUser CurrentUser currentUser
    ) {
        ChatRoomDeleteResponseDto response = chatRoomService.delete(roomId, currentUser.id());
        return ResponseEntity.ok(response);
    }

    // 1:1 문의 채팅방 나가기
    @DeleteMapping("/Inquiries/{roomId}/members/me")
    public ResponseEntity<InquiryMemberLeaveResponseDto> leaveInquiryRoom(
            @PathVariable Long roomId,
            @LoginUser CurrentUser currentUser
    ) {
        return ResponseEntity.ok(inquiryRoomMemberService.leave(roomId, currentUser.id()));
    }

    // 공개 채팅방 조회
    @GetMapping("/fundings/{productId}")
    public ResponseEntity<ChatRoomResponseDto> getFundingRoom(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(chatRoomService.getFundingChatRoomById(productId));
    }
}
