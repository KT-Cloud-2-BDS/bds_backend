package com.bds.notification.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bds.notification.application.dto.FundingNotificationCommandDto;
import com.bds.notification.application.dto.OrderNotificationMessageDto;
import com.bds.notification.common.exception.BusinessException;
import com.bds.notification.common.exception.ErrorCode;
import com.bds.notification.domain.notification.entity.NotificationType;
import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import com.bds.notification.domain.notification.model.Notification;
import com.bds.notification.domain.notification.model.NotificationSubscription;
import com.bds.notification.domain.notification.repository.NotificationRepository;
import com.bds.notification.domain.notification.repository.NotificationSubscriptionRepository;
import com.bds.notification.infrastructure.sse.SseEmitterManager;
import com.bds.notification.presentation.dto.NotificationListResponseDto;
import com.bds.notification.presentation.dto.NotificationSubscribeResponseDto;
import com.bds.notification.presentation.dto.UnreadCountResponseDto;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

  @Mock
  NotificationRepository notificationRepository;

  @Mock
  NotificationSubscriptionRepository notificationSubscriptionRepository;

  @Mock
  SseEmitterManager sseEmitterManager;

  @InjectMocks
  NotificationService notificationService;


  @Nested
  @DisplayName("구독 유닛 테스트")
  class SubscribeTest {

    @Test
    @DisplayName("최초 구독 시 구독에 성공한다.")
    public void 정상_구독_등록() {
      //given
      Long memberId = 1L;
      SubscriptionTargetType targetType = SubscriptionTargetType.PRODUCT;
      Long targetId = 1L;
      when(notificationSubscriptionRepository.existsActiveSubscription(memberId, targetType,
          targetId))
          .thenReturn(false);

      //when
      NotificationSubscribeResponseDto result =
          notificationService.subscribe(memberId, targetType, targetId);

      //then
      assertThat(result.subscribed()).isEqualTo(true);
      assertThat(result.targetId()).isEqualTo(targetId);
      verify(notificationSubscriptionRepository).save(any(NotificationSubscription.class));
    }

    @Test
    @DisplayName("이미 해당 타겟 아이디가 구독 되어있을 시 구독에 실패한다.")
    public void 실패_구독_등록() {
      //given
      Long memberId = 1L;
      SubscriptionTargetType targetType = SubscriptionTargetType.PRODUCT;
      Long targetId = 1L;
      when(notificationSubscriptionRepository.existsActiveSubscription(memberId, targetType,
          targetId))
          .thenReturn(true);
      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> notificationService.subscribe(memberId, targetType, targetId));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
      verify(notificationSubscriptionRepository, never()).save(any(NotificationSubscription.class));
    }
  }

  @Nested
  @DisplayName("구독 해지 유닛 테스트")
  class UnsubscribeTest {

    @Test
    @DisplayName("구독 해지에 성공")
    public void 성공_구독_해지() {
      //given
      // 멤버 아이디, 구독 타입, 타겟 아이디를 받아야함.
      // 구독 목록에 조회 했을때 subscribe 정보를 return 해주어야함.
      Long memberId = 1L;
      SubscriptionTargetType targetType = SubscriptionTargetType.PRODUCT;
      Long targetId = 1L;
      NotificationSubscription notificationSubscription = mock(NotificationSubscription.class);
      when(notificationSubscriptionRepository.findActiveSubscription(
          memberId, targetType, targetId
      )).thenReturn(Optional.of(notificationSubscription));

      //when
      notificationService.unsubscribe(memberId, targetType, targetId);

      //then
      verify(notificationSubscription).softDelete();
      verify(notificationSubscriptionRepository).save(any(NotificationSubscription.class));
    }

    @Test
    @DisplayName("구독 정보가 없어 구독 실패")
    public void 실패_구독정보_미존재() {
      //given
      Long memberId = 1L;
      SubscriptionTargetType targetType = SubscriptionTargetType.PRODUCT;
      Long targetId = 1L;
      when(notificationSubscriptionRepository.findActiveSubscription(
          memberId, targetType, targetId
      )).thenReturn(Optional.empty());

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> notificationService.unsubscribe(memberId, targetType, targetId));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("알림 미읽음 테스트")
  class GetUnreadCountTest {

    @Test
    @DisplayName("정상_미읽음_카운트_조회")
    public void getUnreadCount() {
      //given
      Long memberId = 1L;
      when(notificationRepository.countUnreadByMemberId(memberId))
          .thenReturn(10L);

      //when
      UnreadCountResponseDto dto = notificationService.getUnreadCount(memberId);

      //then
      assertThat(dto.unreadCount()).isEqualTo(10L);
    }
  }

  @Nested
  @DisplayName("알림 리스트 반환 테스트")
  class GetNotificationTest {

    @Test
    @DisplayName("알림 리스트를 정상적으로 반환한다.")
    public void 성공_알림_리스트_반환() {
      //given
      Long memberId = 1L;
      Pageable pageable = PageRequest.of(0, 20);

      Notification notification = mock(Notification.class);
      Page<Notification> notifications = new PageImpl<>(List.of(notification));
      // Page<Notification> notifications = mock(Page.class);

      when(notificationRepository.findByMemberId(memberId, pageable))
          .thenReturn(notifications);

      //when
      NotificationListResponseDto responseDto = notificationService.getNotifications(memberId,
          pageable);

      //then
      assertThat(responseDto.notifications().size()).isEqualTo(notifications.getContent().size());
      verify(notificationRepository).markAllAsRead(memberId);
    }
  }

  @Nested
  @DisplayName("SSE 연결 단위 테스트")
  class ConnectTest {

    @Test
    @DisplayName("SSE 연결 성공 시 SseEmitter을 반환한다.")
    public void SSE_연결_성공() {
      //given
      Long memberId = 1L;
      SseEmitter emitter = mock(SseEmitter.class);
      when(sseEmitterManager.create(memberId)).thenReturn(emitter);

      //when
      SseEmitter sseEmitter = notificationService.connect(memberId);

      //then
      assertThat(sseEmitter).isEqualTo(emitter);
    }

    @Test
    @DisplayName("SSE 전송 실패 시 SSE_SEND_FAILED 예외가 발생한다.")
    public void SSE_전송_실패() throws Exception {
      //given
      Long memberId = 1L;
      SseEmitter emitter = mock(SseEmitter.class);
      when(sseEmitterManager.create(memberId)).thenReturn(emitter);
      doThrow(new IOException()).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> notificationService.connect(memberId));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SSE_SEND_FAILED);
      verify(sseEmitterManager).remove(memberId);
    }
  }

  @Nested
  @DisplayName("주문 상태 알림 생성 테스트")
  class CreateOrderNotificationTest {

    @Test
    @DisplayName("PAID 상태의 주문이 오면 주문 성공 알림이 전송된다.")
    public void 성공_주문완료_알림_생성() {
      //given
      OrderNotificationMessageDto command = new OrderNotificationMessageDto(
          NotificationType.PAID,
          1L,
          "여름 맞이 물총 장난감",
          "order-1234-1234"
      );

      //when
      notificationService.createOrderNotification(command);

      //then
      verify(notificationRepository).save(any(Notification.class));
      verify(sseEmitterManager).send(eq(1L), eq("notification"), any());
    }

    @Test
    @DisplayName("잘못된 타입 요청 시 INVALID_NOTIFICATION_TYPE 예외가 발생한다.")
    public void 실패_잘못된_알림타입() {
      //given
      OrderNotificationMessageDto command = new OrderNotificationMessageDto(
          NotificationType.PROMOTION,
          1L,
          "여름 맞이 물총 장난감",
          "order-1234-1234"
      );

      //when
      BusinessException exception = assertThrows(BusinessException.class,
          () -> notificationService.createOrderNotification(command));

      //then
      assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_NOTIFICATION_TYPE);
    }
  }

  @Nested
  @DisplayName("펀딩 상태 알림")
  class createFundingNotificationTest {

    @Test
    @DisplayName("펀딩이 성공했을 경우 구독한 사용자에게 알림이 간다.")
    public void 성공_펀딩_알림_생성() {
      //given
      FundingNotificationCommandDto command = new FundingNotificationCommandDto(
          NotificationType.FUNDING_START, "123", "PRODUCT"
      );
      when(
          notificationSubscriptionRepository.findSubscribedMemberIds(SubscriptionTargetType.PRODUCT,
              123L))
          .thenReturn(List.of(1L, 2L, 3L));

      //when
      notificationService.createFundingNotification(command);

      //then
      verify(notificationRepository, times(3)).save(any(Notification.class));
      verify(sseEmitterManager, times(3)).send(any(Long.class), eq("notification"), any());
    }

    @Test
    @DisplayName("구독자가 없을 경우 알림이 생성되지 않는다.")
    public void 성공_구독자_없음() {
      //given
      FundingNotificationCommandDto command = new FundingNotificationCommandDto(
          NotificationType.FUNDING_START, "123", "PRODUCT"
      );
      when(
          notificationSubscriptionRepository.findSubscribedMemberIds(SubscriptionTargetType.PRODUCT,
              123L))
          .thenReturn(List.of());

      //when
      notificationService.createFundingNotification(command);

      //then
      verify(notificationRepository, never()).save(any(Notification.class));
    }

  }

}
