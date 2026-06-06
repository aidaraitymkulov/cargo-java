package com.cargoapp.backend.auth.event;

import com.cargoapp.backend.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfirmationEmail(ConfirmationEmailEvent event) {
        try {
            emailService.sendConfirmationCode(event.email(), event.code());
        } catch (MailException ex) {
            log.error("Failed to send confirmation email to {}: {}", event.email(), ex.getMessage(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetEmail(PasswordResetEmailEvent event) {
        try {
            emailService.sendPasswordResetCode(event.email(), event.code());
        } catch (MailException ex) {
            log.error("Failed to send password reset email to {}: {}", event.email(), ex.getMessage(), ex);
        }
    }
}
