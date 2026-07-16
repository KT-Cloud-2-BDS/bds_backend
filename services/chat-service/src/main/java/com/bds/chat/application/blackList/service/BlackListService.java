package com.bds.chat.application.blackList.service;

import com.bds.chat.application.blackList.dto.BlackListCreateRequestDto;
import com.bds.chat.application.blackList.dto.BlackListReponseDto;
import com.bds.chat.common.BusinessException;
import com.bds.chat.common.ErrorCode;
import com.bds.chat.domain.blackList.FundingChatBlacklist;
import com.bds.chat.domain.blackList.FundingChatBlacklistRepository;
import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.shared.MemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BlackListService {

    private final FundingChatBlacklistRepository fundingChatBlacklistRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final Clock clock;

    @Transactional
    public BlackListReponseDto create(Long roomId, Long memberId, BlackListCreateRequestDto request) {
        if (memberId.equals(request.targetId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Cannot ban yourself");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        MemberId requestId = MemberId.of(memberId);
        MemberId banId = MemberId.of(request.targetId());

        ChatRoom room = chatRoomRepository.findActiveById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + roomId));

        if (room.getType() != ChatRoomType.FUNDING) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only funding room can ban members");
        }

        if (!room.getCreatorId().equals(requestId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only creator can ban members");
        }

        if (fundingChatBlacklistRepository.isBlacklisted(roomId, request.targetId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Already banned: memberId=" + request.targetId());
        }

        FundingChatBlacklist blacklist = FundingChatBlacklist.create(room.getId(), banId, request.reason(), now);
        FundingChatBlacklist saved = fundingChatBlacklistRepository.save(blacklist);

        return new BlackListReponseDto(roomId, request.targetId(), saved.getStatus().name());
    }

    @Transactional
    public BlackListReponseDto delete(Long roomId, Long memberId, Long targetId) {
        LocalDateTime now = LocalDateTime.now(clock);
        MemberId requestId = MemberId.of(memberId);

        ChatRoom room = chatRoomRepository.findActiveById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "roomId=" + roomId));

        if (room.getType() != ChatRoomType.FUNDING) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only funding room can release ban");
        }

        if (!room.getCreatorId().equals(requestId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only creator can release ban");
        }

        FundingChatBlacklist blacklist = fundingChatBlacklistRepository.findBlacklist(roomId, targetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "No active ban: memberId=" + targetId));

        blacklist.release(now);
        FundingChatBlacklist saved = fundingChatBlacklistRepository.save(blacklist);

        return new BlackListReponseDto(roomId, targetId, saved.getStatus().name());
    }
}
