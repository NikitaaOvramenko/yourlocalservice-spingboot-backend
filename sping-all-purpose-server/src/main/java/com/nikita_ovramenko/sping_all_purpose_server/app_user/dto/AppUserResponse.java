package com.nikita_ovramenko.sping_all_purpose_server.app_user.dto;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;

/**
 * A user as the API exposes them.
 *
 * <p>Deliberately not the AppUser entity: that carries passwordHash, and serialising it
 * would put a bcrypt hash on the wire.
 */
public record AppUserResponse(Long id, String email, String name, Role role, boolean verified) {
}
