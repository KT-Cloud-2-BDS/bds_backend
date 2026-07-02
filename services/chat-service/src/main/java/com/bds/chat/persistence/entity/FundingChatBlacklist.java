package com.bds.chat.persistence.entity;

import com.bds.chat.common.enums.BlacklistStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "funding_chat_blacklist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingChatBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @Column(nullable = false)
    private Long memberId;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlacklistStatus status;

    @Column(nullable = false)
    private LocalDateTime bannedAt;

    private LocalDateTime deletedAt;

    @Builder
    public FundingChatBlacklist(ChatRoom room, Long memberId, String reason) {
        this.room = room;
        this.memberId = memberId;
        this.reason = reason;
        this.status = BlacklistStatus.ACTIVE;
        this.bannedAt = LocalDateTime.now();
    }

    public void release() {
        this.status = BlacklistStatus.RELEASED;
        this.deletedAt = LocalDateTime.now();
    }
}
