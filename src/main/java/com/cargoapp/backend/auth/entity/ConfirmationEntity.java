package com.cargoapp.backend.auth.entity;

import com.cargoapp.backend.users.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "confirmations")
@Getter
@Setter
@NoArgsConstructor
public class ConfirmationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 4)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfirmationStatus confirmationStatus;

    @Column(nullable = false)
    private int attempts = 3;

    private LocalDateTime expiresAt;
    private LocalDateTime lastSentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
}
