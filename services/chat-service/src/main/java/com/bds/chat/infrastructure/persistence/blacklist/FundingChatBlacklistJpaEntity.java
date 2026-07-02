package com.bds.chat.infrastructure.persistence.blacklist;

import com.bds.chat.domain.blackList.BlacklistStatus;
import com.bds.chat.infrastructure.persistence.chatroom.ChatRoomJpaEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "funding_chat_blacklist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FundingChatBlacklistJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoomJpaEntity room;

    @Column(nullable = false)
    private Long memberId;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlacklistStatus status;

    @Column(nullable = false)
    private LocalDateTime bannedAt;

    private LocalDateTime deletedAt;
}
