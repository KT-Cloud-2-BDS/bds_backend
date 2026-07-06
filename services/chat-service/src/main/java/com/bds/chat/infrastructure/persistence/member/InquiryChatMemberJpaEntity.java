package com.bds.chat.infrastructure.persistence.member;

import com.bds.chat.domain.member.MemberStatus;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry_chat_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InquiryChatMemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoomJpaEntity room;

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
}
