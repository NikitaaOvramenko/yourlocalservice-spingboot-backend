package com.nikita_ovramenko.sping_all_purpose_server.app_user.service;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.RegisterRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserAlreadyExistsException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.AppUserRepo;

@Service
public class AppUserService implements UserDetailsService {

    private final AppUserRepo userRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepo userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AppUser findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("No user with email " + email));
    }

    @Transactional(readOnly = true)
    public List<AppUser> findAll() {
        return userRepository.findAll();
    }

    /**
     * Creates an unverified account.
     *
     * <p>verified stays false deliberately: a self-registered user cannot log in until
     * something flips it. AuthController calls EmailVerificationService straight after
     * this, which emails a link; an administrator can also verify an account directly
     * when that mail cannot be delivered.
     */
    @Transactional
    public AppUser registerUser(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new UserAlreadyExistsException("A user with email " + request.email() + " already exists");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email().trim());
        user.setName(request.name().trim());
        user.setRole(Role.MEMBER);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setVerified(false);

        return userRepository.save(user);
    }

    @Transactional
    public AppUser markVerified(String email) {
        AppUser user = findUserByEmail(email);
        user.setVerified(true);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Used by Spring Security and by JwtFilter on every authenticated request.
     *
     * <p>Note it must throw UsernameNotFoundException rather than return null -- the
     * contract has no null case, and callers do not check for one.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isVerified())
                .build();
    }
}
