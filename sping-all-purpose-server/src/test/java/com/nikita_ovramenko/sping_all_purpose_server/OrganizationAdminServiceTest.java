package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.nikita_ovramenko.sping_all_purpose_server.common.exception.BadRequestException;
import com.nikita_ovramenko.sping_all_purpose_server.organization.dto.MailSettingsRequest;
import com.nikita_ovramenko.sping_all_purpose_server.organization.dto.OrganizationUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.organization.mapper.OrganizationMapper;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.MailSettings;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organization.repository.OrganizationRepo;
import com.nikita_ovramenko.sping_all_purpose_server.organization.service.OrganizationAdminService;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.repository.OrganizationServiceOfferingRepo;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.repository.ServiceOfferingRepo;

class OrganizationAdminServiceTest {
    private final OrganizationRepo organizations = mock(OrganizationRepo.class);
    private final Organization organization = new Organization();
    private final OrganizationAdminService service = new OrganizationAdminService(organizations,
            mock(ServiceOfferingRepo.class), mock(OrganizationServiceOfferingRepo.class), new OrganizationMapper());

    @BeforeEach
    void setUp() {
        organization.setId(1L);
        when(organizations.findById(1L)).thenReturn(Optional.of(organization));
        when(organizations.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void hostAloneNamesMissingFieldsBeforeWriting() {
        var mail = new MailSettingsRequest("smtp.example.com", null, null, null, null, null, null, null);
        assertThatThrownBy(() -> service.update(1L, new OrganizationUpdateRequest(null, null, null, mail)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContainingAll("port", "username", "passwordEnv", "fromEmail");
        verify(organizations, never()).save(any());
    }

    @Test
    void partialMailPatchPreservesExistingSettingsAndReturnsOnlyTheEnvironmentVariableName() {
        MailSettings mail = new MailSettings();
        mail.setHost("smtp.example.com");
        mail.setPort(587);
        mail.setUsername("sender");
        mail.setPasswordEnv("SMTP_PASS_TEST");
        mail.setFromEmail("sender@example.com");
        mail.setStarttlsEnabled(true);
        organization.setMailSettings(mail);
        var response = service.update(1L, new OrganizationUpdateRequest(null, null, null,
                new MailSettingsRequest(null, null, null, null, false, null, null, "New name")));
        assertThat(response.smtpHost()).isEqualTo("smtp.example.com");
        assertThat(response.smtpPort()).isEqualTo(587);
        assertThat(response.smtpPasswordEnv()).isEqualTo("SMTP_PASS_TEST");
        assertThat(response.smtpStarttlsEnabled()).isTrue();
        assertThat(response.smtpSslEnabled()).isFalse();
        assertThat(response.fromName()).isEqualTo("New name");
        assertThat(response.mailConfigured()).isTrue();
    }

    @Test
    void emptyMailPatchDoesNotResetAnExistingSender() {
        MailSettings mail = new MailSettings();
        mail.setHost("smtp.example.com");
        mail.setPort(465);
        mail.setUsername("sender");
        mail.setPasswordEnv("SMTP_PASS_TEST");
        mail.setFromEmail("sender@example.com");
        organization.setMailSettings(mail);
        var response = service.update(1L, new OrganizationUpdateRequest(null, null, null,
                new MailSettingsRequest(null, null, null, null, null, null, null, null)));
        assertThat(response.mailConfigured()).isTrue();
        assertThat(response.smtpHost()).isEqualTo("smtp.example.com");
    }
}
