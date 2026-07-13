package com.bds.chat.config;

import com.bds.chat.domain.chatRoom.ChatRoom;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.chatRoom.ChatRoomStatus;
import com.bds.chat.domain.chatRoom.ChatRoomType;
import com.bds.chat.domain.member.InquiryChatMember;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import com.bds.chat.domain.member.MemberStatus;
import com.bds.chat.domain.message.ChatMessage;
import com.bds.chat.domain.message.ChatMessageRepository;
import com.bds.chat.domain.message.MessageStatus;
import com.bds.chat.domain.message.MessageType;
import com.bds.chat.domain.shared.ChatRoomId;
import com.bds.chat.domain.shared.MemberId;
import com.bds.chat.domain.shared.ProductId;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Component
public class ChatIntegrationTestFixture {
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private InquiryChatMemberRepository inquiryChatMemberRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public ChatRoom createRoom(String name, Long productId, ChatRoomType type, ChatRoomStatus status, Long createdBy) {
        ChatRoom chatRoom = ChatRoom.create(
                MemberId.of(createdBy),                                     // creatorId
                productId != null ? ProductId.of(productId) : null,         // productId
                name,                                                       // title
                type,                                 // type
                LocalDateTime.now()                                        // createdAt
        );
        return chatRoomRepository.save(chatRoom);
    }
    @Transactional
    public InquiryChatMember createMember(Long roomId, Long memberId, MemberStatus status) {
        InquiryChatMember member = InquiryChatMember.create(
                ChatRoomId.of(roomId),                          // roomId
                MemberId.of(memberId),                          // memberId
                LocalDateTime.now()                            // updatedAt
        );
        return inquiryChatMemberRepository.save(member);
    }

    @Transactional
    public ChatMessage createMessage(Long roomId, Long senderId, String clientId) {
        ChatMessage chatMessage = ChatMessage.create(
                ChatRoomId.of(roomId),                              // roomId
                senderId != null ? MemberId.of(senderId) : null,   // senderId (SYSTEM 메시지일 수 있으므로 null 체크)
                "테스트 메시지",                                       // content
                MessageType.valueOf("TEXT"),                        // type (TEXT, IMAGE 등)
                clientId,                                                   // clientId
                LocalDateTime.now()                                // createdAt (기존 Instant에서 LocalDateTime으로 변경)
        );

        return chatMessageRepository.save(chatMessage);
    }

    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM chat_message");
        jdbcTemplate.update("DELETE FROM inquiry_chat_member");
        jdbcTemplate.update("DELETE FROM funding_chat_blacklist");
        jdbcTemplate.update("DELETE FROM chat_room");
    }
}
