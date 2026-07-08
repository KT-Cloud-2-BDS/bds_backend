package com.bds.member.infrastructure.persistence.entity;

import com.bds.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class MemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_id", nullable = false)
    private Long authId;

    @Column(nullable = false, length = 20)
    private String nickname;

    public static MemberJpaEntity from(Member member) {
        return MemberJpaEntity.builder()
            .authId(member.getAuthId())
            .nickname(member.getNickname())
            .build();
    }
}