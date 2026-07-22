package com.bds.chat.infrastructure.messaging;

import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.common.BusinessException;
import com.bds.common.events.funding.FundingStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FundingCreatedListener {
    private final ChatRoomService chatRoomService;
    public FundingCreatedListener(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }
    @RabbitListener(queues = ChatQueues.FUNDING_CREATED, containerFactory = "msaListenerContainerFactory")
    public void handle(FundingStatusChangedEvent event) {
        //markProcessed(event.evenId())를 통한 증복 이벤트 감지 스킵
        log.info("FundingStatusChangedEvent 수신: creatorId={}, targetId={}, type={}", event.creatorId(), event.targetId(), event.type());
        switch (event.type()) {
            case "FUNDING_START" -> {
                try {
                    chatRoomService.createFundingRoom(event.targetId(), new FundingRoomCreateRequestDto(event.creatorId()));
                } catch (BusinessException e) {
                    log.warn("채팅방 생성 비즈니스 오류: {}", e.getMessage());
                } catch (Exception e) {
                    log.error("일시적 오류, 재시도 예정", e);
                    throw e;
                }
            }
            case "FUNDING_SUCCESS", "FUNDING_FAIL" -> {
                try {
                    chatRoomService.delete(event.targetId(), event.creatorId());
                } catch (BusinessException e) {
                    log.warn("채팅방 삭제 비즈니스 오류: {}", e.getMessage());
                } catch (Exception e) {
                    log.error("채팅방 삭제 일시적 오류, 재시도 예정", e);
                    throw e;
                }
            }
        }

    }
}
