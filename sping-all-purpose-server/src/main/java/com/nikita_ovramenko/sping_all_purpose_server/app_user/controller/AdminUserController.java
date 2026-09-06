package com.nikita_ovramenko.sping_all_purpose_server.app_user.controller;

import java.net.URI;
import java.security.Principal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AdminCreateUserRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AppUserResponse;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.UpdateUserRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AdminUserService;
import com.nikita_ovramenko.sping_all_purpose_server.common.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Staff account management.
 *
 * <p>Every response is an AppUserResponse, which has no password field. Nothing here
 * should ever return a hash.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: users", description = "Manage staff accounts and their roles")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "List accounts", description = "Filters are optional and combine with AND.")
    public PageResponse<AppUserResponse> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean verified,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return adminUserService.list(role, verified, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one account")
    public AppUserResponse get(@PathVariable Long id) {
        return adminUserService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create an account directly",
            description = "Creates a verified account by default, so the person can sign in "
                    + "immediately. Note the administrator necessarily knows the initial "
                    + "password and there is no self-service way to change it yet.")
    public ResponseEntity<AppUserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        AppUserResponse created = adminUserService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/users/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Change role or verified flag",
            description = "409 if this would demote your own account or the last remaining "
                    + "administrator. Omitted or null fields are left unchanged.")
    public AppUserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
            Principal principal) {
        return adminUserService.update(id, request, principal == null ? null : principal.getName());
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Mark an account verified without an email round-trip",
            description = "For when verification mail cannot be delivered.")
    public AppUserResponse verify(@PathVariable Long id) {
        return adminUserService.forceVerify(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an account",
            description = "409 if this would delete your own account or the last remaining "
                    + "administrator.")
    public void delete(@PathVariable Long id, Principal principal) {
        adminUserService.delete(id, principal == null ? null : principal.getName());
    }
}
