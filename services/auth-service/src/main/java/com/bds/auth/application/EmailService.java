package com.bds.auth.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.username}")
    private String senderEmail;

    @Async
    public void sendVerificationEmail(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("[bds] 회원가입 이메일 인증번호 안내");
            message.setText("안녕하세요.\n\n" +
                "인증번호 6자리는 다음과 같습니다.\n" +
                "▶ 인증번호: " + code + "\n\n" +
                "3분 이내에 입력해 주세요. 감사합니다.");

            mailSender.send(message);

        } catch (MailException e) {
        }
    }

    @Async
    public void sendPasswordResetVerificationEmail(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("[bds] 비밀번호 재설정 인증번호 안내");
            message.setText("안녕하세요.\n\n" +
                "비밀번호 재설정 인증번호 6자리는 다음과 같습니다.\n" +
                "▶ 인증번호: " + code + "\n\n" +
                "3분 이내에 입력해 주세요. 본인이 요청하지 않았다면 이 메일을 무시해 주세요.");

            mailSender.send(message);

        } catch (MailException e) {
        }
    }
}
