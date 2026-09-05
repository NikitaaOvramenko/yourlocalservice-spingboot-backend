package com.nikita_ovramenko.sping_all_purpose_server.app_user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


public interface AppUserRepo extends JpaRepository<AppUser,Long> {
    AppUser findByEmail(String email); 
}
