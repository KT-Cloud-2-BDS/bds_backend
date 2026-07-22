package com.bds.notification.infrastructure.messaging;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bds.common.events.funding.FundingStatusChangedEvent;
import com.bds.common.events.order.OrderStatusChangedEvent;
import com.bds.notification.application.NotificationService;
import com.bds.notification.domain.notification.entity.NotificationChannel;
import com.bds.notification.domain.notification.entity.NotificationType;
import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import com.bds.notification.infrastructure.persistence.NotificationEntity;
import com.bds.notification.infrastructure.persistence.NotificationJpaRepository;
import com.bds.notification.infrastructure.persistence.NotificationSubscriptionJpaRepository;
import com.bds.notification.infrastructure.sse.SseEmitterManager;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public class ConsumerIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

  @Container
  static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:4-management");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
    registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
    registry.add("spring.rabbitmq.virtual-host", () -> "/");
    registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
    registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
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
  RabbitTemplate rabbitTemplate;

  @Autowired
  NotificationService notificationService;

  @Autowired
  NotificationJpaRepository notificationJpaRepository;

  @Autowired
  NotificationSubscriptionJpaRepository notificationSubscriptionJpaRepository;

  @BeforeEach
  public void setup() {
    notificationJpaRepository.deleteAll();
    notificationSubscriptionJpaRepository.deleteAll();
  }

  @MockitoBean
  SseEmitterManager sseEmitterManager;

  @Nested
  @DisplayName("Order 알림 테스트")
  class Order {

    @Test
    @DisplayName("PAID 메시지 수신 시 알림이 DB에 저장되고 sse 알림이 전달된다.")
    public void 결제완료_메시지_수신_알림_저장() throws InterruptedException {
      //given
      OrderStatusChangedEvent message = new OrderStatusChangedEvent(
          "PAID", 1L, "여름맞이 물총", "order-1234"
      );

      // http 연결이라 mock으로 대체해서 send가 되는지만 확인
      when(sseEmitterManager.exist(1L)).thenReturn(true);

      //when
      rabbitTemplate.convertAndSend(
          RabbitTopologyConfig.ORDER_EXCHANGE,
          RabbitTopologyConfig.ORDER_STATUS_ROUTING_KEY,
          message
      );

      //then
      Thread.sleep(1000);
      assertThat(notificationJpaRepository.count()).isEqualTo(1);
      verify(sseEmitterManager).send(eq(1L), eq("notification"), any());

      NotificationEntity saved = notificationJpaRepository.findAll().get(0);
      assertThat(saved.getMemberId()).isEqualTo(1L);
      assertThat(saved.getType()).isEqualTo(NotificationType.PAID);
      assertThat(saved.getTitle()).isEqualTo("주문이 완료됐어요");
      assertThat(saved.getChannel()).isEqualTo(NotificationChannel.SSE);
    }

    @Test
    @DisplayName("REFUND 메시지 수신 시 알림이 DB에 저장되고 sse 알림이 전달된다.")
    public void 환불_메시지_수신_알림_저장() throws InterruptedException {
      //given
      OrderStatusChangedEvent message = new OrderStatusChangedEvent(
          "REFUNDED", 1L, "여름맞이 물총", "order-1234"
      );

      // http 연결이라 mock으로 대체해서 send가 되는지만 확인
      when(sseEmitterManager.exist(1L)).thenReturn(true);

      //when
      rabbitTemplate.convertAndSend(
          RabbitTopologyConfig.ORDER_EXCHANGE,
          RabbitTopologyConfig.ORDER_STATUS_ROUTING_KEY,
          message
      );

      //then
      Thread.sleep(1000);
      assertThat(notificationJpaRepository.count()).isEqualTo(1);
      verify(sseEmitterManager).send(eq(1L), eq("notification"), any());

      NotificationEntity saved = notificationJpaRepository.findAll().get(0);
      assertThat(saved.getMemberId()).isEqualTo(1L);
      assertThat(saved.getType()).isEqualTo(NotificationType.REFUNDED);
      assertThat(saved.getTitle()).isEqualTo("환불이 완료됐어요");
      assertThat(saved.getChannel()).isEqualTo(NotificationChannel.SSE);
    }
  }

  @Nested
  @DisplayName("Funding 알림 테스트")
  class Funding {

    @Test
    @DisplayName("FUNDING_START 메시지 수신 시 알림이 DB에 저장되고 sse 알림이 전달된다.")
    public void 펀딩시작_메시지_수신_알림_저장() throws InterruptedException {
      //given
      notificationService.subscribe(1L, SubscriptionTargetType.PRODUCT, 1234L);

      FundingStatusChangedEvent message = new FundingStatusChangedEvent(
          "FUNDING_START", "PRODUCT", 1234L, null
      );

      // http 연결이라 mock으로 대체해서 send가 되는지만 확인
      when(sseEmitterManager.exist(1L)).thenReturn(true);

      //when
      rabbitTemplate.convertAndSend(
          RabbitTopologyConfig.FUNDING_EXCHANGE,
          RabbitTopologyConfig.FUNDING_STATUS_ROUTING_KEY,
          message
      );

      //then
      Thread.sleep(1000);
      assertThat(notificationJpaRepository.count()).isEqualTo(1);
      verify(sseEmitterManager).send(eq(1L), eq("notification"), any());

      NotificationEntity saved = notificationJpaRepository.findAll().get(0);
      assertThat(saved.getMemberId()).isEqualTo(1L);
      assertThat(saved.getType()).isEqualTo(NotificationType.FUNDING_START);
      assertThat(saved.getTitle()).isEqualTo("펀딩이 시작되었어요");
      assertThat(saved.getChannel()).isEqualTo(NotificationChannel.SSE);
    }

    @Test
    @DisplayName("FUNDING_SUCCESS 메시지 수신 시 알림이 DB에 저장되고 sse 알림이 전달된다.")
    public void 펀딩성공_메시지_수신_알림_저장() throws InterruptedException {
      //given
      notificationService.subscribe(1L, SubscriptionTargetType.PRODUCT, 1234L);

      FundingStatusChangedEvent message = new FundingStatusChangedEvent(
          "FUNDING_SUCCESS", "PRODUCT", 1234L, null
      );

      // http 연결이라 mock으로 대체해서 send가 되는지만 확인
      when(sseEmitterManager.exist(1L)).thenReturn(true);

      //when
      rabbitTemplate.convertAndSend(
          RabbitTopologyConfig.FUNDING_EXCHANGE,
          RabbitTopologyConfig.FUNDING_STATUS_ROUTING_KEY,
          message
      );

      //then
      Thread.sleep(1000);
      assertThat(notificationJpaRepository.count()).isEqualTo(1);
      verify(sseEmitterManager).send(eq(1L), eq("notification"), any());

      NotificationEntity saved = notificationJpaRepository.findAll().get(0);
      assertThat(saved.getMemberId()).isEqualTo(1L);
      assertThat(saved.getType()).isEqualTo(NotificationType.FUNDING_SUCCESS);
      assertThat(saved.getTitle()).isEqualTo("펀딩에 성공하셨어요");
      assertThat(saved.getChannel()).isEqualTo(NotificationChannel.SSE);
    }

    @Test
    @DisplayName("FUNDING_FAIL 메시지 수신 시 알림이 DB에 저장되고 sse 알림이 전달된다.")
    public void 펀딩실패_메시지_수신_알림_저장() throws InterruptedException {
      //given
      notificationService.subscribe(1L, SubscriptionTargetType.PRODUCT, 1234L);

      FundingStatusChangedEvent message = new FundingStatusChangedEvent(
          "FUNDING_FAIL", "PRODUCT", 1234L, null
      );

      // http 연결이라 mock으로 대체해서 send가 되는지만 확인
      when(sseEmitterManager.exist(1L)).thenReturn(true);

      //when
      rabbitTemplate.convertAndSend(
          RabbitTopologyConfig.FUNDING_EXCHANGE,
          RabbitTopologyConfig.FUNDING_STATUS_ROUTING_KEY,
          message
      );

      //then
      Thread.sleep(1000);
      assertThat(notificationJpaRepository.count()).isEqualTo(1);
      verify(sseEmitterManager).send(eq(1L), eq("notification"), any());

      NotificationEntity saved = notificationJpaRepository.findAll().get(0);
      assertThat(saved.getMemberId()).isEqualTo(1L);
      assertThat(saved.getType()).isEqualTo(NotificationType.FUNDING_FAIL);
      assertThat(saved.getTitle()).isEqualTo("펀딩에 실패하셨어요");
      assertThat(saved.getChannel()).isEqualTo(NotificationChannel.SSE);
    }
  }
}
