package com.cargoapp.backend.managers.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "manager_refresh_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ManagerRefreshSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private ManagerEntity manager;

    @Column(unique = true, nullable = false)
    private String jti;

    private String fingerprint;
    private String ip;
    private String userAgent;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
}
