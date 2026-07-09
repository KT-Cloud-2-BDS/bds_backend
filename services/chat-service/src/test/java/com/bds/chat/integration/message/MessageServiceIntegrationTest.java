package com.bds.chat.integration.message;

import com.bds.chat.application.chatRoom.dto.ChatRoomResponseDto;
import com.bds.chat.application.chatRoom.dto.FundingRoomCreateRequestDto;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.application.message.dto.*;
import com.bds.chat.application.message.service.MessageService;
import com.bds.chat.config.ChatIntegrationTestFixture;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.domain.message.ChatMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@Transactional
@DisplayName("메시지 서비스 통합 테스트")
class MessageServiceIntegrationTest {

    @Autowired private MessageService messageService;
    @Autowired private ChatRoomService chatRoomService;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ChatIntegrationTestFixture fixture;

    private static final Long SELLER_ID = 2L;
    private static final Long BUYER_ID = 7L;
    private static final Long PRODUCT_ID = 10L;

    private Long setupFundingRoom() {
        return chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID)).roomId();
    }

    private long[] setupInquiryRoomWithFunding() {
        Long fundingRoomId = setupFundingRoom();
        Long inquiryRoomId = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID).roomId();
        return new long[]{fundingRoomId, inquiryRoomId};
    }

    @Nested
    @DisplayName("메시지 생성")
    class CreateTest {

        // 펀딩방 메시지 전송 → DTO 검증 + DB 저장 검증
        @Test
        @DisplayName("펀딩 채팅방에 메시지를 전송하면 DB에 저장된다")
        void 펀딩_채팅방에_메시지를_전송하면_DB에_저장된다() {
            Long roomId = setupFundingRoom();
            MessageSendRequestDto request = new MessageSendRequestDto(roomId, "안녕하세요", "TEXT", null);

            MessageResponseDto result = messageService.create(request, BUYER_ID);

            // Step 1: 응답 검증
            assertThat(result.messageId()).isNotNull();
            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.content()).isEqualTo("안녕하세요");
            assertThat(result.isDeleted()).isFalse();

            // Step 2: DB 저장 검증
            assertThat(chatMessageRepository.findById(result.messageId())).isPresent();
        }

        // 문의방 멤버가 메시지 전송 → DTO 검증 + DB 저장 검증
        @Test
        @DisplayName("문의 채팅방 멤버가 메시지를 전송하면 DB에 저장된다")
        void 문의_채팅방_멤버가_메시지를_전송하면_DB에_저장된다() {
            long[] roomIds = setupInquiryRoomWithFunding();
            Long inquiryRoomId = roomIds[1];
            MessageSendRequestDto request = new MessageSendRequestDto(inquiryRoomId, "문의드립니다", "TEXT", null);

            MessageResponseDto result = messageService.create(request, BUYER_ID);

            // Step 1: 응답 검증
            assertThat(result.messageId()).isNotNull();
            assertThat(result.roomId()).isEqualTo(inquiryRoomId);
            assertThat(result.senderId()).isEqualTo(BUYER_ID);

            // Step 2: DB 저장 검증
            assertThat(chatMessageRepository.findById(result.messageId())).isPresent();
        }

        // 동일 clientId 재전송 → 멱등적으로 동일 메시지 반환
        @Test
        @DisplayName("동일 clientId로 재전송 시 멱등적으로 동일 메시지가 반환된다")
        void 동일_clientId로_재전송_시_멱등적으로_동일_메시지가_반환된다() {
            Long roomId = setupFundingRoom();
            String clientId = "client-idempotent-1";
            MessageSendRequestDto request = new MessageSendRequestDto(roomId, "중복 전송", "TEXT", clientId);

            MessageResponseDto first = messageService.create(request, BUYER_ID);
            MessageResponseDto second = messageService.create(request, BUYER_ID);

            assertThat(second.messageId()).isEqualTo(first.messageId());
        }
    }

    @Nested
    @DisplayName("메시지 삭제")
    class DeleteTest {

        // 발신자가 자신의 메시지 삭제 → DTO 검증 + DB 삭제 상태 검증
        @Test
        @DisplayName("발신자가 자신의 메시지를 삭제하면 DB에서 삭제 상태로 변경된다")
        void 발신자가_자신의_메시지를_삭제하면_DB에서_삭제_상태로_변경된다() {
            Long roomId = setupFundingRoom();
            MessageResponseDto created = messageService.create(
                    new MessageSendRequestDto(roomId, "삭제할 메시지", "TEXT", null), BUYER_ID);

            MessageDeleteResponseDto result = messageService.delete(created.messageId(), BUYER_ID);

            // Step 1: 응답 검증
            assertThat(result.messageId()).isEqualTo(created.messageId());
            assertThat(result.isDeleted()).isTrue();
            assertThat(result.deletedAt()).isNotNull();

            // Step 2: DB 삭제 상태 검증
            assertThat(chatMessageRepository.findById(created.messageId()))
                    .isPresent()
                    .get()
                    .satisfies(m -> assertThat(m.isDeleted()).isTrue());
        }
    }

    @Nested
    @DisplayName("채팅 이력 조회")
    class GetHistoryTest {

        // 자신이 보낸 메시지 이력 조회 → 메시지 목록 반환 검증
        @Test
        @DisplayName("자신이 보낸 메시지 이력을 조회할 수 있다")
        void 자신이_보낸_메시지_이력을_조회할_수_있다() {
            Long roomId = setupFundingRoom();
            fixture.createMessage(roomId, BUYER_ID, "history-1");
            fixture.createMessage(roomId, BUYER_ID, "history-2");

            MessageListResponseDto result = messageService.getHistory(BUYER_ID, null);

            assertThat(result.messages()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(result.messages()).allSatisfy(m -> assertThat(m.senderId()).isEqualTo(BUYER_ID));
        }
    }

    @Nested
    @DisplayName("문의방 메시지 조회")
    class GetInquiryMessagesTest {

        // 펀딩방에서 memberId 없이 조회 성공
        @Test
        @DisplayName("펀딩 채팅방의 메시지를 memberId 없이 조회할 수 있다")
        void 펀딩_채팅방_메시지를_memberId_없이_조회할_수_있다() {
            Long roomId = setupFundingRoom();
            fixture.createMessage(roomId, SELLER_ID, "funding-msg-1");

            MessageListResponseDto result = messageService.getInquiryMessages(roomId, null, null);

            assertThat(result.messages()).isNotEmpty();
        }

        // 문의방에서 멤버로 조회 → lastRead 갱신 포함
        @Test
        @DisplayName("문의 채팅방 멤버가 메시지를 조회하면 성공한다")
        void 문의_채팅방_멤버가_메시지를_조회하면_성공한다() {
            long[] roomIds = setupInquiryRoomWithFunding();
            Long inquiryRoomId = roomIds[1];
            fixture.createMessage(inquiryRoomId, BUYER_ID, "inquiry-msg-1");

            MessageListResponseDto result = messageService.getInquiryMessages(inquiryRoomId, BUYER_ID, null);

            assertThat(result.messages()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("펀딩방 메시지 조회")
    class GetFundingMessagesTest {

        // 펀딩방 메시지 조회 성공
        @Test
        @DisplayName("펀딩 채팅방의 메시지를 조회할 수 있다")
        void 펀딩_채팅방의_메시지를_조회할_수_있다() {
            Long roomId = setupFundingRoom();
            fixture.createMessage(roomId, SELLER_ID, "funding-get-1");

            MessageListResponseDto result = messageService.getFundingMessages(roomId, null);

            assertThat(result.messages()).isNotEmpty();
        }
    }
}
