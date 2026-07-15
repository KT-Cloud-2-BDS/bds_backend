package com.bds.auth.service;

import com.bds.auth.application.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService 단위 테스트")
public class EmailServiceUnitTest {

    @InjectMocks
    public EmailService emailService;

    @Mock
    public JavaMailSender mailSender;

    @Test
    @DisplayName("이메일 주소와 인증 코드가 주어지면 인증 메일이 정상 발송된다")
    public void 인증메일발송_성공() {
        // given
        String toEmail = "yeojin@email.com";
        String code = "123456";

        ReflectionTestUtils.setField(emailService, "senderEmail", "sender@bds.com");

        // when
        emailService.sendVerificationEmail(toEmail, code);

        // then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}