package com.nikita_ovramenko.sping_all_purpose_server.app_user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserAlreadyExistsException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserNotFoundException;

import jakarta.transaction.Transactional;

@Service
public class AppUserService implements UserDetailsService {

    private final AppUserRepo userRepository;

    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepo userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser findUserByEmail(String email) {

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("User Not Found !");
        }
        return user;
    }

    @Transactional
    public AppUser registerUser(AppUser user) {

        AppUser exist = userRepository.findByEmail(user.getEmail());

        if (exist != null) {
            throw new UserAlreadyExistsException("User with this email already exists !");
        }

        user.setVerified(false);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        if (user.getRole() == null) {
            user.setRole(Role.MEMBER);
        }

        AppUser saved = userRepository.save(user);
        return saved;
    }

    @Transactional
    public AppUser updateUser(AppUser user) {
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = findUserByEmail(email);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();

    }

}