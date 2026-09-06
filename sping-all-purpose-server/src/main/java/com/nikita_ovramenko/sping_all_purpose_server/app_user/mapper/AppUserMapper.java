package com.nikita_ovramenko.sping_all_purpose_server.app_user.mapper;

import org.springframework.stereotype.Component;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AppUserResponse;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;

/** AppUser -> API response. There is no reverse: registration goes through AppUserService. */
@Component
public class AppUserMapper {

    public AppUserResponse toResponse(AppUser user) {
        return new AppUserResponse(
                user.getId(), user.getEmail(), user.getName(), user.getRole(), user.isVerified());
    }
}
