package com.nikita_ovramenko.sping_all_purpose_server.app_user.exception;

import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ResourceNotFoundException;

/**
 * A user addressed by id that does not exist, rendered as 404.
 *
 * <p>Deliberately not UserNotFoundException, which the exception handler maps to 401
 * because it is thrown from the login path, where distinguishing "no such account" from
 * "wrong password" would let an attacker enumerate registered emails. On an admin
 * lookup by id there is no such concern, and 401 would be simply wrong.
 */
public class AppUserNotFoundException extends ResourceNotFoundException {

    public AppUserNotFoundException(Long id) {
        super("No user with id " + id);
    }
}
