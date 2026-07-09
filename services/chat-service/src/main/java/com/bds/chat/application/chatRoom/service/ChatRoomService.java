package com.bds.chat.application.chatRoom.service;

import com.bds.chat.application.chatRoom.dto.*;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.ChatMessageRepository;
import com.bds.chat.domain.message.LatestWithUnread;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.domain.shared.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final InquiryChatMemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final Clock clock;


    @Transactional
    public ChatRoomResponseDto createInquiryRoom(Long productId, Long buyerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        MemberId buyerMemberId = MemberId.of(buyerId);
        ProductId pid = ProductId.of(productId);

        // 기존 방이 있으면 buyer를 rejoin 처리
        return chatRoomRepository.findInquiryRoomByProductAndBuyer(productId, buyerId)
                .map(existingRoom -> {
                    List<InquiryChatMember> members = memberRepository.findAllByRoomId(existingRoom.getId().value());

                    if (members.stream().noneMatch(m -> m.getMemberId().value().equals(buyerId))) {
                        throw new BusinessException(ErrorCode.NOT_FOUND,
                                "roomId=" + existingRoom.getId().value() + " memberId=" + buyerId);
                    }

                    if (existingRoom.getDeletedAt() != null) {
                        existingRoom.reopen();
                        chatRoomRepository.save(existingRoom);
                    }

                    members.forEach(m -> {
                        m.rejoin(now);
                        memberRepository.save(m);
                    });

                    Long sellerId = existingRoom.getCreatorId().value();
                    return ChatRoomResponseDto.from(existingRoom, List.of(buyerId, sellerId));
                })
                .orElseGet(() -> {
                    MemberId sellerMemberId = findSellerFromFundingRoom(productId);

                    ChatRoom room = chatRoomRepository.save(
                            ChatRoom.create(buyerMemberId, pid, null, ChatRoomType.INQUIRY, now)
                    );
                    memberRepository.save(InquiryChatMember.create(room.getId(), buyerMemberId, now));
                    memberRepository.save(InquiryChatMember.create(room.getId(), sellerMemberId, now));

                    return ChatRoomResponseDto.from(room, List.of(buyerMemberId.value(), sellerMemberId.value()));
                });
    }

    @Transactional
    public ChatRoomResponseDto createFundingRoom(Long productId, FundingRoomCreateRequestDto request) {
        LocalDateTime now = LocalDateTime.now(clock);
        MemberId creatorMemberId = MemberId.of(request.creatorId());
        ProductId pid = ProductId.of(productId);

        guardDuplicateFundingRoom(productId);

        ChatRoom room = chatRoomRepository.save(
                ChatRoom.create(creatorMemberId, pid, null, ChatRoomType.FUNDING, now)
        );

        return ChatRoomResponseDto.from(room, List.of());
    }

    @Transactional
    public ChatRoomDeleteResponseDto delete(Long roomId, Long memberId) {
        LocalDateTime now = LocalDateTime.now(clock);
        MemberId requesterId = MemberId.of(memberId);

        ChatRoom room = findActiveRoomForUpdate(roomId);
        if (!room.getCreatorId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only creator can close room");
        }
        room.delete(now);
        chatRoomRepository.save(room);
        return new ChatRoomDeleteResponseDto(room.getId().value(), true, room.getDeletedAt());
    }

    @Transactional(readOnly = true)
    public InquiryChatRoomDetailResponseDto getInquiryChatRoomById(Long roomId, Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Inquiry room requires authentication");
        }

        ChatRoom room = findActiveInquiryRoom(roomId);
        List<InquiryChatMember> members = memberRepository.findActiveMembers(roomId);

        InquiryChatMember myMember = members.stream()
                .filter(m -> m.getMemberId().value().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "Not a member of this room"));

        List<Long> participants = members.stream()
                .map(m -> m.getMemberId().value())
                .toList();

        ChatMessage latestMessage = chatMessageRepository.findLatestWithUnread(roomId, myMember.getLastReadMessageId()).latest();
        LastMessageDto lastMessage = latestMessage != null ? LastMessageDto.from(latestMessage) : null;

        return InquiryChatRoomDetailResponseDto.from(room, participants, MembershipStatusDto.from(myMember), lastMessage);
    }

    @Transactional(readOnly = true)
    public ChatRoomResponseDto getFundingChatRoomById(Long roomId){
        ChatRoom room = findActiveFundingRoom(roomId);
        return ChatRoomResponseDto.from(room, List.of());
    }

    @Transactional(readOnly = true)
    public InquiryRoomListResponseDto getMyInquiryRooms(Long memberId, Long cursor, int limit) {
        List<InquiryChatMember> memberships = memberRepository.findByMemberId(memberId);
        List<Long> roomIds = memberships.stream()
                .map(m -> m.getRoomId().value())
                .toList();

        if (roomIds.isEmpty()) {
            return InquiryRoomListResponseDto.empty();
        }

        List<ChatRoom> rooms = chatRoomRepository.findActiveByIds(roomIds, cursor, limit + 1);

        boolean hasNext = rooms.size() > limit;
        List<ChatRoom> page = hasNext ? rooms.subList(0, limit) : rooms;
        Long nextCursor = hasNext ? page.get(page.size() - 1).getId().value() : null;

        List<Long> pageRoomIds = page.stream().map(r -> r.getId().value()).toList();

        Map<Long, Long> lastReadByRoom = memberships.stream()
                .collect(HashMap::new,
                        (map, m) -> map.put(m.getRoomId().value(), m.getLastReadMessageId()),
                        HashMap::putAll);

        Map<Long, List<Long>> participantsByRoom = memberRepository.findActiveMembersByRoomIds(pageRoomIds)
                .stream()
                .collect(Collectors.groupingBy(
                        m -> m.getRoomId().value(),
                        Collectors.mapping(m -> m.getMemberId().value(), Collectors.toList())
                ));

        Map<Long, LatestWithUnread> latestByRoom = chatMessageRepository.findLatestWithUnreadBatch(pageRoomIds, lastReadByRoom);

        List<InquiryRoomSummaryDto> summaries = page.stream()
                .map(room -> {
                    Long roomId = room.getId().value();
                    List<Long> participants = participantsByRoom.getOrDefault(roomId, List.of());
                    LatestWithUnread lau = latestByRoom.getOrDefault(roomId, new LatestWithUnread(null, 0L));
                    return InquiryRoomSummaryDto.from(room, participants, lau.latest(), lau.unreadCount());
                })
                .toList();

        return new InquiryRoomListResponseDto(summaries, nextCursor, hasNext, roomIds.size());
    }

    /* -------------------- private -------------------- */

    private void guardDuplicateFundingRoom(Long productId) {
        chatRoomRepository.findFundingRoomByProduct(productId)
                .ifPresent(r -> {
                    throw new BusinessException(ErrorCode.CONFLICT,
                            "Funding room already exists: productId=" + productId);
                });
    }

    private MemberId findSellerFromFundingRoom(Long productId) {
        return chatRoomRepository.findFundingRoomByProduct(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "No funding room for productId=" + productId))
                .getCreatorId();
    }

    private ChatRoom findActiveRoom(Long roomId) {
        return chatRoomRepository.findActiveById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + roomId));
    }

    private ChatRoom findActiveInquiryRoom(Long roomId) {
        ChatRoom room = findActiveRoom(roomId);
        if (room.getType() != ChatRoomType.INQUIRY) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + roomId);
        }
        return room;
    }

    private ChatRoom findActiveFundingRoom(Long roomId) {
        ChatRoom room = findActiveRoom(roomId);
        if (room.getType() != ChatRoomType.FUNDING) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + roomId);
        }
        return room;
    }

    private ChatRoom findActiveRoomForUpdate(Long roomId) {
        return chatRoomRepository.findActiveByIdForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + roomId));
    }
}
