package com.bds.notification.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
// TODO: 도메인 모델로 변경 예정

@Getter
@NoArgsConstructor
@Entity
@Table(name = "fcm_token")
public class FcmToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long fcmTokenId;

  @Column(nullable = false)
  private Long memberId;

  @Column(nullable = false)
  private String token;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Builder
  private FcmToken(Long memberId, String token) {
    this.memberId = memberId;
    this.token = token;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public void updateToken(String token) {
    this.token = token;
    this.updatedAt = LocalDateTime.now();
  }
}