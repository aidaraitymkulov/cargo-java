package com.cargoapp.backend.managers.entity;

import com.cargoapp.backend.branches.entity.BranchEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "managers")
@Getter
@Setter
@NoArgsConstructor
public class ManagerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(nullable = false)
    private String passwordHash;

    /**
     * Пароль в открытом виде — хранится для отображения в админке.
     * Обновляется вместе с passwordHash при смене пароля.
     */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phone;

    /**
     * Роль менеджера: MANAGER или SUPER_ADMIN.
     * Хранится как строка (не FK) — роли фиксированы и не меняются.
     */
    @Column(nullable = false)
    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
