package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AdminCreateUserRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.AppUserResponse;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.dto.UpdateUserRequest;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.enums.Role;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.AppUserNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.mapper.AppUserMapper;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.model.AppUser;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.repository.AppUserRepo;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AdminUserService;
import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ConflictException;

/**
 * Plain unit test -- no Spring context, no database.
 *
 * <p>Mostly about the lockout guards. There is no password-reset flow and no way to
 * create an administrator except through an existing one, so an admin who removes the
 * last admin locks everyone out permanently, fixable only by hand in the database.
 */
class AdminUserServiceTest {

    private static final String CALLER = "admin@example.com";

    private AppUserRepo userRepository;
    private AdminUserService service;
    private AppUser caller;
    private AppUser otherAdmin;
    private AppUser member;

    private static AppUser user(Long id, String email, Role role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail(email);
        user.setName("User " + id);
        user.setRole(role);
        user.setVerified(true);
        user.setPasswordHash("hashed");
        return user;
    }

    @BeforeEach
    void setUp() {
        caller = user(1L, CALLER, Role.ADMIN);
        otherAdmin = user(2L, "second@example.com", Role.ADMIN);
        member = user(3L, "member@example.com", Role.MEMBER);

        userRepository = mock(AppUserRepo.class);
        given(userRepository.findById(1L)).willReturn(Optional.of(caller));
        given(userRepository.findById(2L)).willReturn(Optional.of(otherAdmin));
        given(userRepository.findById(3L)).willReturn(Optional.of(member));
        given(userRepository.save(any(AppUser.class))).willAnswer(call -> call.getArgument(0));

        PasswordEncoder encoder = mock(PasswordEncoder.class);
        given(encoder.encode(any())).willReturn("hashed");

        service = new AdminUserService(userRepository, new AppUserMapper(), encoder);
    }

    @Test
    void adminCannotDemoteTheirOwnAccount() {
        assertThatThrownBy(() -> service.update(1L, new UpdateUserRequest(Role.MEMBER, null), CALLER))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("your own administrator account");

        assertThat(caller.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void adminCannotDeleteTheirOwnAccount() {
        assertThatThrownBy(() -> service.delete(1L, CALLER))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).delete(any(AppUser.class));
    }

    /** The backstop for a call with no authenticated principal to compare against. */
    @Test
    void theLastRemainingAdminCannotBeDemoted() {
        given(userRepository.countByRole(Role.ADMIN)).willReturn(1L);

        assertThatThrownBy(() -> service.update(2L, new UpdateUserRequest(Role.MEMBER, null), null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("only administrator");

        assertThat(otherAdmin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void anotherAdminCanBeDemotedWhileMoreThanOneRemains() {
        given(userRepository.countByRole(Role.ADMIN)).willReturn(2L);

        AppUserResponse updated = service.update(2L, new UpdateUserRequest(Role.MEMBER, null), CALLER);

        assertThat(updated.role()).isEqualTo(Role.MEMBER);
    }

    @Test
    void theLastRemainingAdminCannotBeDeleted() {
        given(userRepository.countByRole(Role.ADMIN)).willReturn(1L);
        assertThatThrownBy(() -> service.delete(2L, CALLER))
                .isInstanceOf(ConflictException.class).hasMessageContaining("only administrator");
        verify(userRepository, never()).delete(any(AppUser.class));
    }

    /** The guards are about administrators; an ordinary account is not protected. */
    @Test
    void aMemberCanBeDeletedFreely() {
        service.delete(3L, CALLER);

        verify(userRepository).delete(member);
    }

    @Test
    void demotingSomeoneWhoIsAlreadyAMemberIsNotBlocked() {
        service.update(3L, new UpdateUserRequest(Role.MEMBER, null), CALLER);

        assertThat(member.getRole()).isEqualTo(Role.MEMBER);
    }

    /** An admin creating an account has already established who the person is. */
    @Test
    void createdAccountsAreVerifiedByDefault() {
        given(userRepository.existsByEmailIgnoreCase("new@example.com")).willReturn(false);

        AppUserResponse created = service.create(new AdminCreateUserRequest(
                "new@example.com", "New Person", "correcthorse", Role.MEMBER, null));

        assertThat(created.verified()).isTrue();
        assertThat(created.role()).isEqualTo(Role.MEMBER);
    }

    @Test
    void createdAccountsCanBeLeftUnverified() {
        given(userRepository.existsByEmailIgnoreCase("new@example.com")).willReturn(false);

        AppUserResponse created = service.create(new AdminCreateUserRequest(
                "new@example.com", "New Person", "correcthorse", Role.MEMBER, false));

        assertThat(created.verified()).isFalse();
    }

    /** Must be a 404, not the 401 that the login-path UserNotFoundException produces. */
    @Test
    void anUnknownIdIsReportedAsNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(AppUserNotFoundException.class);
    }
}
