package com.bds.chat.e2e;

import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("STOMP WebSocket E2E 테스트")
class ChatWebSocketE2ETest extends ChatE2ETestBase {

    @Autowired
    private ChatRoomService chatRoomService;

    private static final Long SELLER_ID = 2L;
    private static final Long BUYER_ID = 7L;
    private static final Long OTHER_ID = 99L;  // 문의방 비멤버
    private static final Long PRODUCT_ID = 9999L;

    private Long fundingRoomId;
    private Long inquiryRoomId;

    // 테스트별 세션 추적 — @AfterEach에서 일괄 disconnect
    private final List<StompSession> sessions = new ArrayList<>();

    @BeforeEach
    void setUpRooms() {
        fundingRoomId = chatRoomService.createFundingRoom(
                PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID)).roomId();
        inquiryRoomId = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID).roomId();
    }

    @AfterEach
    void disconnectSessions() {
        sessions.forEach(s -> {
            if (s.isConnected()) s.disconnect();
        });
        sessions.clear();
    }

    private StompSession openSession(String token) throws Exception {
        StompSession s = connect(token);
        sessions.add(s);
        return s;
    }

    private StompSession openAnonymousSession() throws Exception {
        StompSession s = connectAnonymous();
        sessions.add(s);
        return s;
    }

    @Nested
    @DisplayName("WebSocket 연결")
    class ConnectTest {

        // 유효한 JWT로 CONNECT → 세션 연결 성공
        @Test
        @DisplayName("유효한 JWT로 STOMP 연결에 성공한다")
        void 유효한_JWT로_STOMP_연결에_성공한다() throws Exception {
            StompSession session = openSession(createToken(BUYER_ID));

            assertThat(session.isConnected()).isTrue();
        }

        // 유효하지 않은 JWT로 CONNECT → 서버가 ERROR 프레임 반환, 연결 거부
        @Test
        @DisplayName("유효하지 않은 JWT로 연결하면 거부된다")
        void 유효하지_않은_JWT로_연결하면_거부된다() {
            StompHeaders headers = new StompHeaders();
            headers.add("Authorization", "Bearer INVALID.TOKEN.HERE");

            assertThatThrownBy(() ->
                    stompClient.connectAsync(wsUrl(), new WebSocketHttpHeaders(), headers,
                            new StompSessionHandlerAdapter() {}).get(3, TimeUnit.SECONDS)
            ).isInstanceOf(ExecutionException.class);
        }
    }

    @Nested
    @DisplayName("구독 권한")
    class SubscribeTest {

        // 익명 사용자 → FUNDING 구독 허용, 이후 메시지 수신 가능
        @Test
        @DisplayName("익명 사용자가 FUNDING 채팅방을 구독하면 메시지를 수신한다")
        void 익명_사용자가_FUNDING_채팅방을_구독하면_메시지를_수신한다() throws Exception {
            StompSession anonSession = openAnonymousSession();
            BlockingQueue<Map<String, Object>> queue = subscribeRoom(anonSession, fundingRoomId);
            Thread.sleep(300);

            StompSession sellerSession = openSession(createToken(SELLER_ID));
            sendMessage(sellerSession, fundingRoomId, "익명 수신 테스트");

            Map<String, Object> msg = queue.poll(3, TimeUnit.SECONDS);
            assertThat(msg).isNotNull();
            assertThat(msg.get("content")).isEqualTo("익명 수신 테스트");
        }

        // 인증 사용자 → FUNDING 구독 허용
        @Test
        @DisplayName("인증 사용자가 FUNDING 채팅방을 구독할 수 있다")
        void 인증_사용자가_FUNDING_채팅방을_구독할_수_있다() throws Exception {
            StompSession session = openSession(createToken(BUYER_ID));
            subscribe(session, "/topic/chat.room." + fundingRoomId);

            Thread.sleep(300);
            assertThat(session.isConnected()).isTrue();
        }

        // 비멤버가 INQUIRY 구독 시도 → ERROR 프레임 수신, 연결 종료
        @Test
        @DisplayName("문의방 비멤버가 INQUIRY 채팅방을 구독하면 연결이 종료된다")
        void 문의방_비멤버가_INQUIRY_채팅방을_구독하면_연결이_종료된다() throws Exception {
            StompSession session = openSession(createToken(OTHER_ID));
            subscribe(session, "/topic/chat.room." + inquiryRoomId);

            awaitDisconnected(session, 3);
            assertThat(session.isConnected()).isFalse();
        }

        // ACTIVE 멤버 → INQUIRY 구독 허용
        @Test
        @DisplayName("ACTIVE 멤버가 INQUIRY 채팅방을 구독할 수 있다")
        void ACTIVE_멤버가_INQUIRY_채팅방을_구독할_수_있다() throws Exception {
            StompSession session = openSession(createToken(BUYER_ID));
            subscribe(session, "/topic/chat.room." + inquiryRoomId);

            Thread.sleep(300);
            assertThat(session.isConnected()).isTrue();
        }
    }

    @Nested
    @DisplayName("메시지 전송 및 수신")
    class MessageTest {

        // 발신자 메시지 전송 → 구독 중인 다른 사용자가 content, senderId 포함하여 수신
        @Test
        @DisplayName("메시지를 전송하면 구독자가 content와 senderId를 포함하여 수신한다")
        void 메시지를_전송하면_구독자가_content와_senderId를_포함하여_수신한다() throws Exception {
            StompSession receiverSession = openSession(createToken(BUYER_ID));
            BlockingQueue<Map<String, Object>> queue = subscribeRoom(receiverSession, fundingRoomId);
            Thread.sleep(300);

            StompSession senderSession = openSession(createToken(SELLER_ID));
            sendMessage(senderSession, fundingRoomId, "E2E 메시지");

            Map<String, Object> msg = queue.poll(3, TimeUnit.SECONDS);
            assertThat(msg).isNotNull();
            assertThat(msg.get("content")).isEqualTo("E2E 메시지");
            assertThat(msg.get("senderId")).isEqualTo(String.valueOf(SELLER_ID));
        }

        // 발신자 자신도 seq 포함하여 수신 (브로드캐스트이므로 자기 자신도 수신)
        @Test
        @DisplayName("메시지를 전송하면 자신도 seq와 함께 수신한다")
        void 메시지를_전송하면_자신도_seq와_함께_수신한다() throws Exception {
            StompSession session = openSession(createToken(SELLER_ID));
            BlockingQueue<Map<String, Object>> queue = subscribeRoom(session, fundingRoomId);
            Thread.sleep(300);

            sendMessage(session, fundingRoomId, "자기 수신 테스트");

            Map<String, Object> msg = queue.poll(3, TimeUnit.SECONDS);
            assertThat(msg).isNotNull();
            assertThat(msg.get("seq")).isNotNull();
            assertThat(msg.get("content")).isEqualTo("자기 수신 테스트");
        }

        // 읽음 처리 → /topic/chat.room.{roomId}.read에 ReadEvent 브로드캐스트
        @Test
        @DisplayName("읽음 처리를 하면 ReadEvent가 구독자에게 전달된다")
        void 읽음_처리를_하면_ReadEvent가_구독자에게_전달된다() throws Exception {
            StompSession session = openSession(createToken(BUYER_ID));
            BlockingQueue<Map<String, Object>> readQueue = subscribeReadReceipt(session, fundingRoomId);
            Thread.sleep(300);

            sendReadReceipt(session, fundingRoomId, 42L);

            Map<String, Object> event = readQueue.poll(3, TimeUnit.SECONDS);
            assertThat(event).isNotNull();
            assertThat(((Number) event.get("userId")).longValue()).isEqualTo(BUYER_ID);
            assertThat(((Number) event.get("lastReadMessageId")).longValue()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class TokenRenewalTest {

        // 만료 임박 토큰으로 접속 시 TOKEN_RENEWAL_REQUIRED 수신 (5초 뒤 발송)
        @Test
        @DisplayName("토큰 만료가 임박하면 TOKEN_RENEWAL_REQUIRED를 수신한다")
        void 토큰_만료가_임박하면_TOKEN_RENEWAL_REQUIRED를_수신한다() throws Exception {
            StompSession session = openSession(createShortLivedToken(BUYER_ID));
            BlockingQueue<Map<String, Object>> authQueue = subscribeAuth(session);
            Thread.sleep(1000);

            Map<String, Object> msg = authQueue.poll(6, TimeUnit.SECONDS);
            assertThat(msg).isNotNull();
            assertThat(msg.get("type")).isEqualTo("TOKEN_RENEWAL_REQUIRED");
        }

        // 갱신 요청 성공 → TOKEN_REFRESHED 수신 후 메시지 전송 가능
        @Test
        @DisplayName("토큰 갱신 성공 시 TOKEN_REFRESHED를 수신하고 이후 메시지를 전송할 수 있다")
        void 토큰_갱신_성공_시_TOKEN_REFRESHED를_수신하고_이후_메시지를_전송할_수_있다() throws Exception {
            StompSession session = openSession(createShortLivedToken(BUYER_ID));
            BlockingQueue<Map<String, Object>> authQueue = subscribeAuth(session);
            BlockingQueue<Map<String, Object>> roomQueue = subscribeRoom(session, fundingRoomId);

            Thread.sleep(1000);
            // TOKEN_RENEWAL_REQUIRED 대기
            Map<String, Object> renewalMsg = authQueue.poll(6, TimeUnit.SECONDS);
            assertThat(renewalMsg).isNotNull();
            assertThat(renewalMsg.get("type")).isEqualTo("TOKEN_RENEWAL_REQUIRED");

            // 새 토큰으로 갱신
            sendTokenRefresh(session, createToken(BUYER_ID));

            // TOKEN_REFRESHED 수신 확인
            Map<String, Object> refreshedMsg = authQueue.poll(3, TimeUnit.SECONDS);
            assertThat(refreshedMsg).isNotNull();
            assertThat(refreshedMsg.get("type")).isEqualTo("TOKEN_REFRESHED");

            // 갱신 후 메시지 전송 가능 확인
            sendMessage(session, fundingRoomId, "갱신 후 전송");
            Map<String, Object> msg = roomQueue.poll(3, TimeUnit.SECONDS);
            assertThat(msg).isNotNull();
            assertThat(msg.get("content")).isEqualTo("갱신 후 전송");
        }

        // Grace Period(5s) 초과 → SESSION_TERMINATED 이후 세션 종료
        @Test
        @DisplayName("Grace Period 초과 시 세션이 종료된다")
        void Grace_Period_초과_시_세션이_종료된다() throws Exception {
            StompSession session = openSession(createShortLivedToken(BUYER_ID));
            BlockingQueue<Map<String, Object>> authQueue = subscribeAuth(session);

            Thread.sleep(1000);
            // TOKEN_RENEWAL_REQUIRED 확인 (갱신하지 않음)
            Map<String, Object> renewalMsg = authQueue.poll(6, TimeUnit.SECONDS);
            assertThat(renewalMsg).isNotNull();
            assertThat(renewalMsg.get("type")).isEqualTo("TOKEN_RENEWAL_REQUIRED");

            // renew-grace=PT5S → 갱신 알림 후 5초 뒤 SESSION_TERMINATED + 연결 종료
            awaitDisconnected(session, 12);
            assertThat(session.isConnected()).isFalse();
        }
    }
}
