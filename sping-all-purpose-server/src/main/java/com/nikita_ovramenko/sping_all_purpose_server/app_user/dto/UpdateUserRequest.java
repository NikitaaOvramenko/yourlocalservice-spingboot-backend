package com.nikita_ovramenko.sping_all_purpose_server.app_user.dto;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;

/**
 * Partial update; a null field is left unchanged.
 *
 * <p>There is no password field on purpose. Changing someone else's password is a
 * different operation with different consequences, and this API has no
 * change-your-own-password flow for the affected user to follow afterwards.
 */
public record UpdateUserRequest(
        Role role,
        Boolean verified) {
}
