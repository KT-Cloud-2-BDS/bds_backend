package com.bds.chat.presentation.stomp;

import com.bds.chat.application.message.dto.MessageResponseDto;
import com.bds.chat.application.message.dto.MessageSendRequestDto;
import com.bds.chat.application.message.service.MessageService;
import com.bds.chat.common.DuplicateClientIdException;
import com.bds.chat.domain.message.MessageType;
import com.bds.chat.infrastructure.config.StompPrincipal;
import com.bds.chat.infrastructure.session.ReadReceiptBuffer;
import com.bds.chat.presentation.stomp.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatStompController {

    private final MessageService messageService;
    private final ReadReceiptBuffer readReceiptBuffer;
    private final SimpMessagingTemplate messagingTemplate;


    @MessageMapping("/chat/send/{roomId}")
    public void send(@DestinationVariable Long roomId,
                     @Payload ChatSendRequest request,
                     Principal principal) {
        if(!(principal instanceof StompPrincipal sp)){
            throw new MessagingException("인증되지 않은 세션");
        }
        Long senderId = Long.parseLong(sp.getName());
        ChatMessageDto message = new ChatMessageDto(
                request.clientMessageId(), roomId, principal.getName(), request.content(), Instant.now());

        try {
            MessageResponseDto saved = messageService.create(
                    new MessageSendRequestDto(roomId, request.content(), MessageType.TEXT.name(), request.clientMessageId()),
                    senderId);

            messagingTemplate.convertAndSend(
                    "/topic/chat.room." + roomId, message.withSeq(saved.messageId()));

        } catch (DuplicateClientIdException e) {
            MessageResponseDto existing = messageService.findByClientId(e.getClientId());
            messagingTemplate.convertAndSend(
                    "/topic/chat.room." + roomId, message.withSeq(existing.messageId()));
        }
        catch (Exception e) {
            log.error("메시지 저장 실패 clientMessageId={} roomId={}",
                    request.clientMessageId(), roomId, e);
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/error",
                    new SendFailure(request.clientMessageId(), "SAVE_FAILED"));
        }
    }

    @MessageMapping("/chat/read/{roomId}")
    public void read(@DestinationVariable Long roomId,
                     @Payload ReadRequest request,
                     Principal principal) {
        if(!(principal instanceof StompPrincipal sp)){
            throw new MessagingException("인증되지 않은 세션");
        }
        Instant readAt = Instant.now();
        Long userId = Long.parseLong(sp.getName());

        readReceiptBuffer.mark(roomId, userId, request.lastReadMessageId(), readAt);
        messagingTemplate.convertAndSend(
                "/topic/chat.room." + roomId + ".read",
                new ReadEvent(roomId, userId, request.lastReadMessageId(), readAt));
    }


    public record ChatSendRequest(String clientMessageId, String content) {}

    public record SendFailure(String clientMessageId, String reason) {}

    public record ReadRequest(Long lastReadMessageId) {}

    public record ReadEvent(Long roomId, Long userId, Long lastReadMessageId, Instant readAt) {}
}
