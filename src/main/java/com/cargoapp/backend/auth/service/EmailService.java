package com.cargoapp.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendConfirmationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Код подтверждения — CargoApp");
        message.setText("Ваш код подтверждения: " + code);
        mailSender.send(message);
        log.info("Confirmation email sent to {}", toEmail);
    }

    public void sendPasswordResetCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Сброс пароля — CargoApp");
        message.setText("Ваш код для сброса пароля: " + code + "\nКод действителен 5 минут.");
        mailSender.send(message);
        log.info("Password reset email sent to {}", toEmail);
    }
}
