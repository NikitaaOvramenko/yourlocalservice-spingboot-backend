package com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nikita_ovramenko.sping_all_purpose_server.organization.mapper.OrganizationMapper;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.dto.ServiceOfferingRequest;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.dto.ServiceOfferingUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.dto.ServiceSummary;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.exception.ServiceOfferingNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.repository.ServiceOfferingRepo;

/**
 * The global service catalogue.
 *
 * <p>Global is the important word: two organizations can offer the same row, so editing
 * a name or description here changes it everywhere that row is offered. There is no
 * delete, because quote and job line items reference services directly and removing one
 * would either fail on the foreign key or erase what past work consisted of.
 */
@Service
public class ServiceOfferingAdminService {

    private final ServiceOfferingRepo serviceOfferingRepo;
    private final OrganizationMapper mapper;

    public ServiceOfferingAdminService(ServiceOfferingRepo serviceOfferingRepo,
            OrganizationMapper mapper) {
        this.serviceOfferingRepo = serviceOfferingRepo;
        this.mapper = mapper;
    }

    /** Includes inactive services, unlike the public per-organization listing. */
    @Transactional(readOnly = true)
    public List<ServiceSummary> list() {
        return serviceOfferingRepo.findAll(Sort.by("name")).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Transactional
    public ServiceSummary create(ServiceOfferingRequest request) {
        ServiceOffering service = new ServiceOffering();
        service.setName(request.name().trim());
        service.setSlug(request.slug().trim().toLowerCase());
        service.setDescription(request.description());
        service.setActive(request.active() == null || request.active());
        return mapper.toSummary(serviceOfferingRepo.save(service));
    }

    /** Partial update: a null field is left unchanged. slug is not changeable. */
    @Transactional
    public ServiceSummary update(Long id, ServiceOfferingUpdateRequest request) {
        ServiceOffering service = serviceOfferingRepo.findById(id)
                .orElseThrow(() -> new ServiceOfferingNotFoundException(id));

        if (request.name() != null) {
            service.setName(request.name().trim());
        }
        if (request.description() != null) {
            service.setDescription(request.description());
        }
        if (request.active() != null) {
            service.setActive(request.active());
        }

        return mapper.toSummary(serviceOfferingRepo.save(service));
    }
}
