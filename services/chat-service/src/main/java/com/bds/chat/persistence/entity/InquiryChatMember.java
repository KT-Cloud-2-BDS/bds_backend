package com.bds.chat.persistence.entity;

import com.bds.chat.common.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry_chat_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    private Long lastReadMessageId;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder
    public InquiryChatMember(ChatRoom room, Long memberId) {
        this.room = room;
        this.memberId = memberId;
        this.status = MemberStatus.ACTIVE;
        this.joinedAt = LocalDateTime.now();
    }

    public void updateLastRead(Long messageId) {
        this.lastReadMessageId = messageId;
        this.updatedAt = LocalDateTime.now();
    }

    public void leave() {
        this.status = MemberStatus.LEFT;
        this.deletedAt = LocalDateTime.now();
    }
}
