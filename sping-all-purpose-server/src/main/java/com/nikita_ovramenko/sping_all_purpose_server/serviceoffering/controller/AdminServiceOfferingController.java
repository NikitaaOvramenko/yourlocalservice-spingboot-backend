package com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.dto.ServiceOfferingRequest;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.dto.ServiceOfferingUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.dto.ServiceSummary;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.service.ServiceOfferingAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * The global service catalogue, shared across organizations.
 *
 * <p>No DELETE: quote and job line items reference services directly, so removing a row
 * would either fail on the foreign key or erase what past work consisted of. Set
 * active=false to retire one.
 */
@RestController
@RequestMapping("/api/admin/services")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: service catalogue", description = "The services organizations can offer")
public class AdminServiceOfferingController {

    private final ServiceOfferingAdminService serviceOfferingAdminService;

    public AdminServiceOfferingController(ServiceOfferingAdminService serviceOfferingAdminService) {
        this.serviceOfferingAdminService = serviceOfferingAdminService;
    }

    @GetMapping
    @Operation(summary = "List the whole catalogue, including inactive services")
    public List<ServiceSummary> list() {
        return serviceOfferingAdminService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a service to the catalogue",
            description = "Shared: any organization can then be given this service.")
    public ServiceSummary create(@Valid @RequestBody ServiceOfferingRequest request) {
        return serviceOfferingAdminService.create(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a catalogue entry",
            description = "Affects every organization offering it. Omitted or null fields are "
                    + "left unchanged; the slug cannot be changed.")
    public ServiceSummary update(@PathVariable Long id,
            @Valid @RequestBody ServiceOfferingUpdateRequest request) {
        return serviceOfferingAdminService.update(id, request);
    }
}
