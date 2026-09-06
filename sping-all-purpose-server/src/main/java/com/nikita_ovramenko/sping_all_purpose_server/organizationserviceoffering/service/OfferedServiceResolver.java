package com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.exception.ServiceNotOfferedException;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.repository.OrganizationServiceOfferingRepo;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.exception.UnknownServiceException;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.repository.ServiceOfferingRepo;

/**
 * Resolves service ids to catalog rows and checks the organization actually offers them.
 *
 * <p>Two distinct failures, kept distinct on purpose: not in the catalog at all is a 400
 * (the caller sent a bad id), while a real service this organization does not offer is a
 * 422 (the request is well-formed, the combination is not valid).
 *
 * <p>Shared by quote submission, quote line editing and job creation, so all three agree
 * on what "this org can do that" means.
 */
@Service
public class OfferedServiceResolver {

    private final ServiceOfferingRepo serviceOfferingRepo;
    private final OrganizationServiceOfferingRepo organizationServiceOfferingRepo;

    public OfferedServiceResolver(ServiceOfferingRepo serviceOfferingRepo,
            OrganizationServiceOfferingRepo organizationServiceOfferingRepo) {
        this.serviceOfferingRepo = serviceOfferingRepo;
        this.organizationServiceOfferingRepo = organizationServiceOfferingRepo;
    }

    public ServiceOffering requireOffered(Organization organization, Long serviceId) {
        return requireAllOffered(organization, List.of(serviceId)).get(serviceId);
    }

    public Map<Long, ServiceOffering> requireAllOffered(
            Organization organization, Collection<Long> serviceIds) {

        Set<Long> requested = new LinkedHashSet<>(serviceIds);

        List<ServiceOffering> found = serviceOfferingRepo.findAllById(requested);
        // findAllById silently drops ids it cannot find, so the size check is required.
        if (found.size() != requested.size()) {
            Set<Long> foundIds = found.stream().map(ServiceOffering::getId).collect(Collectors.toSet());
            Set<Long> unknown = new LinkedHashSet<>(requested);
            unknown.removeAll(foundIds);
            throw new UnknownServiceException(unknown);
        }

        Set<Long> offered = organizationServiceOfferingRepo
                .findServiceIdsByOrganizationId(organization.getId());
        Set<Long> notOffered = new LinkedHashSet<>(requested);
        notOffered.removeAll(offered);
        if (!notOffered.isEmpty()) {
            throw new ServiceNotOfferedException(organization.getSlug(), notOffered);
        }

        return found.stream().collect(Collectors.toMap(ServiceOffering::getId, Function.identity()));
    }
}
