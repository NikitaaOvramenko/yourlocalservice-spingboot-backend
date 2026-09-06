package com.nikita_ovramenko.sping_all_purpose_server.job.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.common.dto.PageResponse;
import com.nikita_ovramenko.sping_all_purpose_server.job.dto.JobCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.job.dto.JobResponse;
import com.nikita_ovramenko.sping_all_purpose_server.job.dto.JobSummary;
import com.nikita_ovramenko.sping_all_purpose_server.job.dto.JobUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.job.enums.JobStatus;
import com.nikita_ovramenko.sping_all_purpose_server.job.service.JobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Scheduling and tracking work.
 *
 * <p>No DELETE: a job that will not happen is moved to CANCELLED, which keeps it in the
 * record. Deleting would also orphan any review attached to it.
 */
@RestController
@RequestMapping("/api/admin/jobs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: jobs", description = "Turn accepted quotes into work, and track it")
public class AdminJobController {

    private final JobService jobService;

    public AdminJobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @Operation(summary = "List jobs",
            description = "Newest first. All filters are optional and combine with AND.")
    public PageResponse<JobSummary> list(
            @RequestParam(name = "orgSlug", required = false) String organizationSlug,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String clientEmail,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return jobService.list(organizationSlug, status, clientEmail, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one job in full, including its line items")
    public JobResponse get(@PathVariable Long id) {
        return jobService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a job, from a quote or as walk-in work",
            description = "Send quoteId alone to convert an accepted quote, copying its "
                    + "client, location and priced lines. Otherwise send organizationSlug, "
                    + "client, location and services. 409 if the quote already has a job.")
    public ResponseEntity<JobResponse> create(@Valid @RequestBody JobCreateRequest request) {
        JobResponse created = jobService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/jobs/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update status, schedule or description",
            description = "Omitted or null fields are left unchanged.")
    public JobResponse update(@PathVariable Long id, @Valid @RequestBody JobUpdateRequest request) {
        return jobService.update(id, request);
    }
}
