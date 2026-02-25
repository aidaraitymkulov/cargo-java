package com.cargoapp.backend.auth.config;

import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.repository.UserRepository;
import com.cargoapp.backend.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(String... args) {
        if (userRepository.existsByLogin(adminProperties.getLogin())) {
            return;
        }

        var role = userRoleRepository.findByRoleName("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("Role SUPER_ADMIN not found"));

        UserEntity admin = new UserEntity();
        admin.setLogin(adminProperties.getLogin());
        admin.setEmail(adminProperties.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(adminProperties.getPassword()));
        admin.setFirstName("Super");
        admin.setLastName("Admin");
        admin.setPhone("");
        admin.setDateOfBirth(java.time.LocalDate.of(2000, 1, 1));
        admin.setRole(role);

        userRepository.save(admin);
    }
}
