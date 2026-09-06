package com.nikita_ovramenko.sping_all_purpose_server.app_user.dto;

/**
 * Tokens plus who they belong to, so a client does not have to decode the JWT just to
 * show a name or branch on a role.
 */
public record AuthResponse(String accessToken, String refreshToken, AppUserResponse user) {
    @Override
    public String toString() { return "AuthResponse[tokens=REDACTED]"; }
}
