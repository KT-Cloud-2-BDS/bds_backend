package com.bds.chat.application.member.service;

import com.bds.chat.domain.member.InquiryChatMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class InquiryRoomMemberService {
    private final InquiryChatMemberRepository inquiryChatMemberRepository;
    private final Clock clock;

}
