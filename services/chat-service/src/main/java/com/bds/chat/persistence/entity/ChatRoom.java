package com.bds.chat.persistence.entity;

import com.bds.chat.common.enums.ChatRoomStatus;
import com.bds.chat.common.enums.ChatRoomType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long creatorId;

    @Column(nullable = false)
    private Long productId;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomType type;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @Builder
    public ChatRoom(Long creatorId, Long productId, String title, ChatRoomStatus status, ChatRoomType type) {
        this.creatorId = creatorId;
        this.productId = productId;
        this.title = title;
        this.status = status;
        this.type = type;
    }

    public void close() {
        this.status = ChatRoomStatus.CLOSED;
        this.deletedAt = LocalDateTime.now();
    }
}
