package com.bds.chat.application.chatRoom.service;

import com.bds.chat.application.chatRoom.ChatRoomAccessPolicy;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomAccessService implements ChatRoomAccessPolicy {
    private final ChatRoomRepository chatRoomRepository;
    private final InquiryChatMemberRepository inquiryChatMemberRepository;

    public boolean canSubscribe(Long roomId, Optional<String> userId){
        ChatRoom chatRoom = chatRoomRepository.findActiveById(roomId).orElse(null);
        if (chatRoom == null) {
            log.warn("구독 거부 — 존재하지 않거나 비활성 방 roomId={}", roomId);
            return false;
        }
        return switch (chatRoom.getType()){
            case FUNDING -> true;
            case INQUIRY ->
                userId.flatMap(this::parseMemberId)                    // 익명(empty)·비정상 sub → 거부
                        .map(memberId -> isInquiryMember(roomId, memberId))
                        .orElseGet(() -> {
                            log.warn("구독 거부 — 문의방 익명/비정상 접근 roomId={}", roomId);
                            return false;
                        });
        };
    }

    private Optional<Long> parseMemberId(String userId) {
        try {
            return Optional.of(Long.parseLong(userId));
        } catch (NumberFormatException e) {
            log.warn("구독 거부 — 숫자가 아닌 userId(sub): {}", userId);
            return Optional.empty();
        }
    }

    private boolean isInquiryMember(Long roomId, Long memberId) {
        boolean member = inquiryChatMemberRepository.existsActiveMember(roomId, memberId);
        if (!member) {
            log.warn("구독 거부 — 문의방 비멤버 또는 비활성 roomId={} memberId={}", roomId, memberId);
        }
        return member;
    }


}
