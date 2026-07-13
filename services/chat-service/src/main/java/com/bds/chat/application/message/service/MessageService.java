package com.bds.chat.application.message.service;

import com.bds.chat.application.message.dto.*;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import com.bds.chat.domain.blackList.FundingChatBlacklistRepository;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.domain.member.ReadReceipt;
import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.ChatMessageRepository;
import com.bds.chat.domain.message.MessageType;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int PAGE_SIZE = 20;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final InquiryChatMemberRepository memberRepository;
    private final FundingChatBlacklistRepository blacklistRepository;
    private final Clock clock;

    @Transactional
    public MessageResponseDto create(MessageSendRequestDto request, Long senderId) {
        if (request.clientId() != null) {
            return chatMessageRepository.findByClientId(request.clientId())
                    .map(MessageResponseDto::from)
                    .orElseGet(() -> doSend(request, senderId));
        }
        return doSend(request, senderId);
    }

    @Transactional
    public MessageDeleteResponseDto delete(Long messageId, Long memberId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "messageId=" + messageId));

        if (message.isDeleted()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "messageId=" + messageId);
        }

        ChatRoom room = chatRoomRepository.findActiveById(message.getRoomId().value())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + message.getRoomId().value()));

        MemberId id = MemberId.of(memberId);
        boolean isSender = message.getSenderId() != null && message.getSenderId().equals(id);
        boolean isCreator = room.getCreatorId().equals(id);

        if (!isSender && !isCreator) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot delete other user's message");
        }

        message.delete(LocalDateTime.now(clock));
        return MessageDeleteResponseDto.from(chatMessageRepository.save(message));
    }

    // 채팅 이력 조회 (자신이 보낸 메시지 전체)
    @Transactional(readOnly = true)
    public MessageListResponseDto getHistory(Long memberId, Long cursor) {
        List<ChatMessage> messages = chatMessageRepository.findBySenderIdBefore(memberId, cursor, PAGE_SIZE + 1);
        return toPagedResponse(messages);
    }


    @Transactional
    public MessageListResponseDto getInquiryMessages(Long roomId, Long memberId, Long cursor) {
        ChatRoom room = chatRoomRepository.findActiveById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + roomId));

        Optional<InquiryChatMember> memberOpt = memberId != null
                ? memberRepository.findActiveMember(roomId, memberId)
                : Optional.empty();

        boolean isInquiryRoom = ChatRoomType.INQUIRY == room.getType();
        boolean isMember = memberOpt.isPresent();

        if(!isInquiryRoom){
            throw new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + roomId);
        }

        if(!isMember){
            throw new BusinessException(ErrorCode.FORBIDDEN, "Not a member of this room");
        }


        List<ChatMessage> messages = chatMessageRepository.findByRoomIdBefore(roomId, cursor, PAGE_SIZE + 1);
        MessageListResponseDto response = toPagedResponse(messages);

        if (!messages.isEmpty() && isMember) {
            InquiryChatMember member = memberOpt.orElseThrow();
            member.updateLastRead(messages.getFirst().getId().value(), LocalDateTime.now(clock));
            memberRepository.save(member);
        }

        return response;
    }


    @Transactional(readOnly = true)
    public MessageResponseDto findByClientId(String clientId) {
        return chatMessageRepository.findByClientId(clientId)
                .map(MessageResponseDto::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "clientId=" + clientId));
    }

    @Transactional
    public void upsertAllReadReceipts(List<ReadReceipt> batch) {
        if (batch.isEmpty()) {
            return;
        }
        memberRepository.bulkUpdateLastRead(batch);
    }

    @Transactional(readOnly = true)
    public MessageListResponseDto getFundingMessages(Long roomId, Long cursor) {
        findActiveRoomByType(roomId, ChatRoomType.FUNDING);
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdBefore(roomId, cursor, PAGE_SIZE + 1);
        return toPagedResponse(messages);
    }

    /* -------------------- private -------------------- */

    private MessageResponseDto doSend(MessageSendRequestDto request, Long senderId) {
        ChatRoom room = chatRoomRepository.findActiveById(request.roomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + request.roomId()));

        MessageType type = parseType(request.type());
        guardSendPermission(room, senderId);

        ChatMessage message = ChatMessage.create(
                ChatRoomId.of(request.roomId()),
                MemberId.of(senderId),
                request.content(),
                type,
                request.clientId(),
                LocalDateTime.now(clock)
        );
        return MessageResponseDto.from(chatMessageRepository.save(message));
    }

    private void guardSendPermission(ChatRoom room, Long senderId) {
        if (room.getType() == ChatRoomType.INQUIRY) {
            memberRepository.findActiveMember(room.getId().value(), senderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "Cannot send message to this room"));
        } else if (room.getType() == ChatRoomType.FUNDING) {
            if (blacklistRepository.isBlacklisted(room.getId().value(), senderId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Banned user cannot send messages");
            }
        }
    }

    private ChatRoom findActiveRoomByType(Long roomId, ChatRoomType expectedType) {
        ChatRoom room = chatRoomRepository.findActiveById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + roomId));
        if (!room.getType().equals(expectedType)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + roomId);
        }
        return room;
    }

    private MessageListResponseDto toPagedResponse(List<ChatMessage> messages) {
        boolean hasNext = messages.size() > PAGE_SIZE;
        List<ChatMessage> page = hasNext ? messages.subList(0, PAGE_SIZE) : messages;
        Long nextCursor = hasNext ? page.getLast().getId().value() : null;
        return new MessageListResponseDto(
                page.stream().map(MessageResponseDto::from).toList(),
                nextCursor,
                hasNext,
                page.size()
        );
    }

    private MessageType parseType(String type) {
        try {
            return MessageType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "type=" + type);
        }
    }
}
