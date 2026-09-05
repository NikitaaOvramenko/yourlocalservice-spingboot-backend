package com.nikita_ovramenko.sping_all_purpose_server.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.AppUserRepo;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.Role;


@Component
public class DataInit implements CommandLineRunner {

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
        // Create admin user if not exists
        if (userRepository.findByEmail("tcs.ovramenko@gmail.com") == null) {
            AppUser admin = AppUser.builder().email("tcs.ovramenko@gmail.com").passwordHash(passwordEncoder.encode(adminPassword)).role(Role.ADMIN).build();
            userRepository.save(admin);
        }
    }
}