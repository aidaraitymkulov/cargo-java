package com.cargoapp.backend.auth.entity;

import com.cargoapp.backend.users.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_resets")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 4)
    private String code;

    private UUID resetToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PasswordResetStatus status;

    @Column(nullable = false)
    private int attempts = 3;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
