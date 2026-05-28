package com.cargoapp.backend.auth.event;

import com.cargoapp.backend.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfirmationEmail(ConfirmationEmailEvent event) {
        emailService.sendConfirmationCode(event.email(), event.code());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetEmail(PasswordResetEmailEvent event) {
        emailService.sendPasswordResetCode(event.email(), event.code());
    }
}
