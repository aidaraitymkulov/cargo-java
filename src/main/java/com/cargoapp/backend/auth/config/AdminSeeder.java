package com.cargoapp.backend.auth.config;

import com.cargoapp.backend.common.constants.ManagerRole;
import com.cargoapp.backend.managers.entity.ManagerEntity;
import com.cargoapp.backend.managers.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Создаёт начального SUPER_ADMIN в таблице managers при первом запуске,
 * если учётная запись с таким логином ещё не существует.
 */
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(String... args) {
        if (managerRepository.existsByLogin(adminProperties.getLogin())) {
            return;
        }

        ManagerEntity admin = new ManagerEntity();
        admin.setLogin(adminProperties.getLogin());
        admin.setPasswordHash(passwordEncoder.encode(adminProperties.getPassword()));
        admin.setFirstName("Super");
        admin.setLastName("Admin");
        admin.setPhone("");
        admin.setRole(ManagerRole.SUPER_ADMIN.name());
        // branch = null для SUPER_ADMIN — доступ ко всем филиалам

        managerRepository.save(admin);
    }
}
