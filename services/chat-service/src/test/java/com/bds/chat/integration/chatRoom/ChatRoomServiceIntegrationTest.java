package com.bds.chat.integration.chatRoom;

import com.bds.chat.application.chatRoom.dto.*;
import com.bds.chat.application.chatRoom.service.ChatRoomService;
import com.bds.chat.config.TestContainersConfig;
import com.bds.chat.domain.chatRoom.ChatRoomRepository;
import com.bds.chat.domain.member.InquiryChatMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@Transactional
@DisplayName("채팅방 서비스 통합 테스트")
class ChatRoomServiceIntegrationTest {

    @Autowired private ChatRoomService chatRoomService;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private InquiryChatMemberRepository memberRepository;

    private static final Long SELLER_ID = 2L;
    private static final Long BUYER_ID = 7L;
    private static final Long PRODUCT_ID = 5L;

    @Nested
    @DisplayName("펀딩 채팅방 생성")
    class CreateFundingRoomTest {

        // 정상 생성 → 응답 DTO 및 DB 저장 검증
        @Test
        @DisplayName("펀딩 채팅방 생성 시 DB에 저장된다")
        void 펀딩_채팅방_생성_시_DB에_저장된다() {
            ChatRoomResponseDto result = chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));

            // Step 1: 응답 검증
            assertThat(result.roomId()).isNotNull();
            assertThat(result.type()).isEqualTo("FUNDING");
            assertThat(result.productId()).isEqualTo(PRODUCT_ID);
            assertThat(result.createdBy()).isEqualTo(SELLER_ID);
            assertThat(result.status()).isEqualTo("ACTIVE");

            // Step 2: DB 저장 검증
            assertThat(chatRoomRepository.findFundingRoomByProduct(PRODUCT_ID)).isPresent();
        }
    }

    @Nested
    @DisplayName("문의 채팅방 생성")
    class CreateInquiryRoomTest {

        // 정상 생성 → DTO 검증 및 멤버 자동 생성 DB 검증
        @Test
        @DisplayName("문의 채팅방 생성 시 DB에 저장되고 멤버 2명이 자동 생성된다")
        void 문의_채팅방_생성_시_DB에_저장되고_멤버가_자동_생성된다() {
            chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
            ChatRoomResponseDto result = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID);

            // Step 1: 응답 검증
            assertThat(result.roomId()).isNotNull();
            assertThat(result.type()).isEqualTo("INQUIRY");
            assertThat(result.participants()).containsExactlyInAnyOrder(BUYER_ID, SELLER_ID);

            // Step 2: DB 방 저장 검증
            assertThat(chatRoomRepository.findInquiryRoomByProductAndBuyer(PRODUCT_ID, BUYER_ID)).isPresent();

            // Step 3: DB 멤버 자동 생성 검증
            assertThat(memberRepository.findActiveMembers(result.roomId())).hasSize(2);
        }
    }

    @Nested
    @DisplayName("채팅방 삭제")
    class DeleteTest {

        // 방장이 삭제 → DTO 검증 + findActiveById 조회 불가 검증
        @Test
        @DisplayName("방장이 채팅방을 삭제하면 DB에서 조회되지 않는다")
        void 방장이_채팅방을_삭제하면_DB에서_조회되지_않는다() {
            ChatRoomResponseDto created = chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
            Long roomId = created.roomId();

            ChatRoomDeleteResponseDto result = chatRoomService.delete(roomId, SELLER_ID);

            // Step 1: 응답 검증
            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.isDeleted()).isTrue();
            assertThat(result.deletedAt()).isNotNull();

            // Step 2: DB 삭제 검증
            assertThat(chatRoomRepository.findActiveById(roomId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("펀딩 채팅방 단건 조회")
    class GetFundingChatRoomByIdTest {

        // 정상 조회 → 응답 DTO 검증
        @Test
        @DisplayName("펀딩 채팅방 단건 조회에 성공한다")
        void 펀딩_채팅방_단건_조회에_성공한다() {
            ChatRoomResponseDto created = chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));

            ChatRoomResponseDto result = chatRoomService.getFundingChatRoomById(created.roomId());

            assertThat(result.roomId()).isEqualTo(created.roomId());
            assertThat(result.type()).isEqualTo("FUNDING");
            assertThat(result.productId()).isEqualTo(PRODUCT_ID);
        }
    }

    @Nested
    @DisplayName("문의 채팅방 상세 조회")
    class GetInquiryChatRoomByIdTest {

        // 멤버로서 문의방 조회 → 응답 DTO 검증
        @Test
        @DisplayName("멤버가 문의 채팅방 상세 조회에 성공한다")
        void 멤버가_문의_채팅방_상세_조회에_성공한다() {
            chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
            ChatRoomResponseDto inquiry = chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID);
            Long roomId = inquiry.roomId();

            InquiryChatRoomDetailResponseDto result = chatRoomService.getInquiryChatRoomById(roomId, BUYER_ID);

            assertThat(result.roomId()).isEqualTo(roomId);
            assertThat(result.type()).isEqualTo("INQUIRY");
            assertThat(result.participants()).containsExactlyInAnyOrder(BUYER_ID, SELLER_ID);
            assertThat(result.myMembership()).isNotNull();
        }
    }

    @Nested
    @DisplayName("내 문의 채팅방 목록 조회")
    class GetMyInquiryRoomsTest {

        // 문의방 생성 후 목록 조회 → 방 목록 검증
        @Test
        @DisplayName("내 문의 채팅방 목록을 조회할 수 있다")
        void 내_문의_채팅방_목록을_조회할_수_있다() {
            chatRoomService.createFundingRoom(PRODUCT_ID, new FundingRoomCreateRequestDto(SELLER_ID));
            chatRoomService.createInquiryRoom(PRODUCT_ID, BUYER_ID);

            InquiryRoomListResponseDto result = chatRoomService.getMyInquiryRooms(BUYER_ID, null, 20);

            assertThat(result.rooms()).isNotEmpty();
            assertThat(result.totalCount()).isGreaterThan(0);
        }
    }
}
