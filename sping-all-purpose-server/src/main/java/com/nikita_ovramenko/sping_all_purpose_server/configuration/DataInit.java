package com.nikita_ovramenko.sping_all_purpose_server.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.AppUserRepo;

/**
 * Seeds the one account that can actually sign in.
 *
 * <p>Registration creates unverified users and there is no verification flow yet, so
 * without this there would be no way to obtain a token at all.
 */
@Component
public class DataInit implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInit.class);
    private static final String ADMIN_EMAIL = "tcs.ontario@gmail.com";

    private final AppUserRepo userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.app.admin_password}")
    private String adminPassword;

    public DataInit(AppUserRepo userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            return;
        }

        AppUser admin = AppUser.builder()
                .email(ADMIN_EMAIL)
                .name("Administrator")   // NOT NULL -- omitting it failed the insert
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .verified(true)          // otherwise login rejects the only usable account
                .build();

        userRepository.save(admin);
        log.info("Seeded admin account {}", ADMIN_EMAIL);
    }
}
