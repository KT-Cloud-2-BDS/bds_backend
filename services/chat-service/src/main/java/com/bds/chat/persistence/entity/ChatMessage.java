package com.bds.chat.persistence.entity;

import com.bds.chat.common.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @Column(nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @Column(length = 100)
    private String clientId;

    @Builder
    public ChatMessage(ChatRoom room, Long senderId, String content, String clientId) {
        this.room = room;
        this.senderId = senderId;
        this.content = content;
        this.status = MessageStatus.SENT;
        this.clientId = clientId;
    }

    public void delete() {
        this.status = MessageStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
