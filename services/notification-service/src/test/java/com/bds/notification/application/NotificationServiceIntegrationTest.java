package com.bds.notification.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bds.notification.application.dto.FundingNotificationCommandDto;
import com.bds.notification.application.dto.OrderNotificationMessageDto;
import com.bds.notification.common.exception.BusinessException;
import com.bds.notification.common.exception.ErrorCode;
import com.bds.notification.domain.notification.entity.NotificationChannel;
import com.bds.notification.domain.notification.entity.NotificationType;
import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import com.bds.notification.infrastructure.persistence.NotificationEntity;
import com.bds.notification.infrastructure.persistence.NotificationJpaRepository;
import com.bds.notification.infrastructure.persistence.NotificationSubscriptionJpaRepository;
import com.bds.notification.presentation.dto.NotificationListResponseDto;
import com.bds.notification.presentation.dto.NotificationSubscribeResponseDto;
import com.bds.notification.presentation.dto.UnreadCountResponseDto;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public class NotificationServiceIntegrationTest {

  @Container
  public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
      "postgres:18");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
  }

  @BeforeAll
  static void runMigration(@Autowired DataSource dataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }

  @Autowired
  NotificationService notificationService;

  @Autowired
  NotificationJpaRepository notificationRepository;

  @Autowired
  NotificationSubscriptionJpaRepository notificationSubscriptionRepository;

  @BeforeEach
  void setUp() {
    notificationRepository.deleteAll();
    notificationSubscriptionRepository.deleteAll();
  }

  @Nested
  @DisplayName("구독 등록 통합 테스트")
  class SubscribeIntegrationTests {

    @Test
    @DisplayName("구독 등록 시 DB에 구독 정보가 저장된다.")
    public void 구독_등록_성공() {
      //given
      Long memberId = 1L;
      SubscriptionTargetType targetType = SubscriptionTargetType.PRODUCT;
      Long targetId = 1L;

      //when
      NotificationSubscribeResponseDto dto = notificationService.subscribe(
          memberId, targetType, targetId);

      //then
      boolean subscribed = notificationSubscriptionRepository.existsByMemberIdAndTargetTypeAndTargetId(
          memberId, targetType, targetId);
      assertThat(subscribed).isTrue();
    }

    @Test
    @DisplayName("중복 구독 시 예외가 발생한다.")
    public void 중복_구독_예외발생() {
      //given
      Long memberId = 1L;
      SubscriptionTargetType targetType = SubscriptionTargetType.PRODUCT;
      Long targetId = 1L;
      notificationService.subscribe(memberId, targetType, targetId);

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> notificationService.subscribe(memberId, targetType, targetId));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
      assertThat(notificationSubscriptionRepository.count()).isEqualTo(1L);
    }


  }

  @Nested
  @DisplayName("구독 해지 통합 테스트")
  class UnsubscribeIntegrationTests {

    @Test
    @DisplayName("구독 해지 후 재구독 시 성공한다.")
    public void 구독_해지_후_재구독_성공() {
      // given -> 구독 후 해지
      Long memberId = 1L;
      SubscriptionTargetType targetType = SubscriptionTargetType.PRODUCT;
      Long targetId = 100L;
      notificationService.subscribe(memberId, targetType, targetId);
      notificationService.unsubscribe(memberId, targetType, targetId);

      // when
      NotificationSubscribeResponseDto dto = notificationService.subscribe(memberId, targetType,
          targetId);

      // then
      assertThat(dto.subscribed()).isTrue();
      assertThat(notificationSubscriptionRepository.count()).isEqualTo(1L);
    }
  }

  @Nested
  @DisplayName("알림 목록 조회 통합 테스트")
  class GetNotificationsIntegrationTests {

    @Test
    @DisplayName("알림 목록 조회 시 전체 읽음 처리가 된다.")
    public void 알림_목록_조회_후_전체_읽음_처리() {
      //given
      Long memberId = 1L;
      for (int i = 0; i < 10; i++) {
        createNotification(memberId, (long) i);
      }

      //when
      NotificationListResponseDto dto = notificationService.getNotifications(memberId,
          Pageable.ofSize(20));

      //then
      assertThat(dto.notifications().size()).isEqualTo(10);
      assertThat(dto.unreadCount()).isEqualTo(10L);
      assertThat(notificationRepository.countByMemberIdAndIsReadFalse(memberId)).isEqualTo(0L);
    }
  }

  @Nested
  @DisplayName("미읽음 카운트 조회 통합 테스트")
  class UnreadCountIntegrationTests {

    @Test
    @DisplayName("미읽음 알림 수만 반환")
    public void 미읽음_알림_수_조회() {
      //given
      Long memberId = 1L;
      for (int i = 0; i < 10; i++) {
        createNotification(memberId, (long) i);
      }

      //when
      UnreadCountResponseDto dto = notificationService.getUnreadCount(memberId);

      //then
      assertThat(dto.unreadCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("기존 알림을 다 읽은 후 새로운 알림 추가시 추가된 알림만 반환")
    public void 추가된_미읽음_알림_수_조회() {
      //given
      Long memberId = 1L;
      for (int i = 0; i < 10; i++) {
        createNotification(memberId, (long) i);
      }
      notificationService.getNotifications(memberId, Pageable.ofSize(20));

      for (int i = 10; i < 20; i++) {
        createNotification(memberId, (long) i);
      }

      //when
      UnreadCountResponseDto dto = notificationService.getUnreadCount(memberId);

      //then
      assertThat(dto.unreadCount()).isEqualTo(10L);

    }
  }

  @Nested
  @DisplayName("펀딩 상태 알림 생성 통합 테스트")
  class CreateFundingNotificationIntegrationTests {

    @Test
    @DisplayName("구독자가 있을 때 구독자 수만큼 알림이 DB에 저장된다.")
    public void 펀딩_알림_구독자수만큼_저장() {
      // given - 구독자 3명 등록
      notificationService.subscribe(1L, SubscriptionTargetType.PRODUCT, 123L);
      notificationService.subscribe(2L, SubscriptionTargetType.PRODUCT, 123L);
      notificationService.subscribe(3L, SubscriptionTargetType.PRODUCT, 123L);

      FundingNotificationCommandDto command = new FundingNotificationCommandDto(
          NotificationType.FUNDING_START, "123", "PRODUCT"
      );
      // when
      notificationService.createFundingNotification(command);
      // then
      assertThat(notificationRepository.count()).isEqualTo(3L);
    }

    @Test
    @DisplayName("구독자가 없을 때 알림이 저장되지 않는다.")
    public void 펀딩_알림_구독자없음_저장안됨() {
      // given
      FundingNotificationCommandDto command = new FundingNotificationCommandDto(
          NotificationType.FUNDING_START, "123", "PRODUCT"
      );
      // when
      notificationService.createFundingNotification(command);
      // then
      assertThat(notificationRepository.count()).isEqualTo(0L);
    }
  }

  @Nested
  @DisplayName("주문 상태 알림 생성 통합 테스트")
  class CreateOrderNotificationIntegrationTests {

    @Test
    @DisplayName("PAID 타입으로 알림 생성 시 DB에 저장된다.")
    public void 주문완료_알림_DB_저장() {
      // given
      OrderNotificationMessageDto command = new OrderNotificationMessageDto(
          NotificationType.PAID, 1L, "여름맞이 물총 장난감", "order-1234-1234"
      );
      // when
      notificationService.createOrderNotification(command);
      // then
      assertThat(notificationRepository.count()).isEqualTo(1L);
    }
  }

  private NotificationEntity createNotification(Long memberId, Long targetId) {
    return notificationRepository.save(NotificationEntity.builder()
        .memberId(memberId)
        .type(NotificationType.FUNDING_START)
        .targetId(String.valueOf(targetId))
        .title("제목" + targetId)
        .body("펀딩이 시작되었습니다." + targetId)
        .channel(NotificationChannel.SSE)
        .sendStatus(false)
        .isRead(false)
        .createdAt(java.time.LocalDateTime.now())
        .build());
  }
}
