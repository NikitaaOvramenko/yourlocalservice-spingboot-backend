package com.nikita_ovramenko.sping_all_purpose_server.organization.mapper;

import org.springframework.stereotype.Component;

import com.nikita_ovramenko.sping_all_purpose_server.organization.dto.OrganizationDetail;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.MailSettings;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.dto.ServiceSummary;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

/** Organization and ServiceOffering to their admin-facing responses. */
@Component
public class OrganizationMapper {

    public OrganizationDetail toDetail(Organization organization) {
        MailSettings mail = organization.getMailSettings();

        return new OrganizationDetail(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getContactEmail(),
                organization.isActive(),
                mail != null && mail.isConfigured(),
                mail == null ? null : mail.getHost(),
                mail == null ? null : mail.getPort(),
                mail == null ? null : mail.getUsername(),
                // The environment variable NAME, not the password.
                mail == null ? null : mail.getPasswordEnv(),
                mail == null ? null : mail.getSslEnabled(),
                mail == null ? null : mail.getStarttlsEnabled(),
                mail == null ? null : mail.getFromEmail(),
                mail == null ? null : mail.getFromName());
    }

    public ServiceSummary toSummary(ServiceOffering service) {
        return new ServiceSummary(service.getId(), service.getName(), service.getSlug(),
                service.getDescription());
    }
}
