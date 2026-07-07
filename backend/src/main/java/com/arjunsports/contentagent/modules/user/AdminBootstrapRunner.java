package com.arjunsports.contentagent.modules.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the first ADMIN account from environment variables when the users
 * table is empty, so the system is usable on a fresh database without a
 * hardcoded credential baked into a migration script. No-ops (with a
 * warning) if the bootstrap credentials aren't configured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-bootstrap.email:}")
    private String bootstrapEmail;

    @Value("${app.admin-bootstrap.password:}")
    private String bootstrapPassword;

    @Value("${app.admin-bootstrap.first-name:Academy}")
    private String bootstrapFirstName;

    @Value("${app.admin-bootstrap.last-name:Admin}")
    private String bootstrapLastName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
            log.warn("No users exist and ADMIN_BOOTSTRAP_EMAIL/ADMIN_BOOTSTRAP_PASSWORD are not set. "
                    + "Skipping admin bootstrap - set these environment variables and restart to create the first admin.");
            return;
        }

        User admin = User.builder()
                .firstName(bootstrapFirstName)
                .lastName(bootstrapLastName)
                .email(bootstrapEmail)
                .password(passwordEncoder.encode(bootstrapPassword))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();

        userRepository.save(admin);
        log.info("Bootstrapped first admin account: {}", bootstrapEmail);
    }
}
