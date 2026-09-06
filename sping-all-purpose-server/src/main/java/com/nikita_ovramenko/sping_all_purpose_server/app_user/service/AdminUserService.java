package com.nikita_ovramenko.sping_all_purpose_server.app_user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AdminCreateUserRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AppUserResponse;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.UpdateUserRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.AppUserNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserAlreadyExistsException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.mapper.AppUserMapper;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.AppUserRepo;
import com.nikita_ovramenko.sping_all_purpose_server.common.dto.PageResponse;
import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ConflictException;
import com.nikita_ovramenko.sping_all_purpose_server.common.service.Specs;

/**
 * Staff account management.
 *
 * <p>Two guards run on every demotion and delete, and both exist to prevent an
 * irrecoverable state: this application has no password-reset flow and no way to create
 * an administrator except through an existing one, so losing the last admin would mean
 * losing access permanently, fixable only by hand in the database.
 *
 * <p>A demotion takes effect immediately rather than when the token expires, because
 * JwtFilter reloads authorities from the database on every request instead of trusting
 * the role claim in the token.
 */
@Service
public class AdminUserService {

    private final AppUserRepo userRepository;
    private final AppUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(AppUserRepo userRepository, AppUserMapper userMapper,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<AppUserResponse> list(Role role, Boolean verified, Pageable pageable) {
        Specification<AppUser> spec = Specs.allOfNonNull(
                role == null ? null : (root, query, cb) -> cb.equal(root.get("role"), role),
                verified == null ? null : (root, query, cb) -> cb.equal(root.get("verified"), verified));

        Page<AppUser> page = userRepository.findAll(spec, pageable);
        return PageResponse.of(page, userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AppUserResponse get(Long id) {
        return userMapper.toResponse(require(id));
    }

    @Transactional
    public AppUserResponse create(AdminCreateUserRequest request) {
        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException("A user with email " + email + " already exists");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // An admin creating the account has already established who this is, so the
        // default is a usable account rather than one waiting on an email.
        user.setVerified(request.verified() == null || request.verified());

        return userMapper.toResponse(userRepository.save(user));
    }

    /** Partial update: a null field is left unchanged. */
    @Transactional
    public AppUserResponse update(Long id, UpdateUserRequest request, String callerEmail) {
        AppUser user = require(id);

        if (request.role() != null && request.role() != user.getRole()) {
            if (user.getRole() == Role.ADMIN) {
                guardAdminRemoval(user, callerEmail, "demote");
            }
            user.setRole(request.role());
        }
        if (request.verified() != null) {
            user.setVerified(request.verified());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Marks an account usable without the person following an emailed link.
     *
     * <p>Present because verification depends on the shared SMTP sender, and an
     * undeliverable email should not be able to permanently strand a new colleague.
     */
    @Transactional
    public AppUserResponse forceVerify(Long id) {
        AppUser user = require(id);
        user.setVerified(true);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, String callerEmail) {
        AppUser user = require(id);
        if (user.getRole() == Role.ADMIN) {
            guardAdminRemoval(user, callerEmail, "delete");
        }
        userRepository.delete(user);
    }

    /**
     * Refuses the two ways an administrator can lock everyone out: removing their own
     * access, or removing the only remaining administrator.
     */
    private void guardAdminRemoval(AppUser target, String callerEmail, String verb) {
        if (callerEmail != null && callerEmail.equalsIgnoreCase(target.getEmail())) {
            throw new ConflictException("You cannot " + verb + " your own administrator account. "
                    + "Ask another administrator to do it.");
        }
        if (userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ConflictException("This is the only administrator account, so it cannot be "
                    + verb + "d. Promote another user to ADMIN first.");
        }
    }

    private AppUser require(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new AppUserNotFoundException(id));
    }
}
