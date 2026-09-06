package com.nikita_ovramenko.sping_all_purpose_server.quote.controller;

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
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.AdminQuoteCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteSummary;
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quote.enums.QuoteStatus;
import com.nikita_ovramenko.sping_all_purpose_server.quote.service.QuoteAdminService;
import com.nikita_ovramenko.sping_all_purpose_server.quote.service.QuoteSubmissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Staff-facing quote management.
 *
 * <p>Mounted under /api/admin rather than /api/orgs/{slug}/quotes on purpose:
 * SecurityConfig permits all of GET /api/orgs/** for the public site, so a read endpoint
 * placed there would publish every client's name, email, phone and address.
 *
 * <p>There is deliberately no DELETE. A quote is retired by moving its status to
 * DECLINED or EXPIRED, which keeps history a job or a report may still need.
 */
@RestController
@RequestMapping("/api/admin/quotes")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: quotes", description = "Read, price and progress submitted quotes")
public class AdminQuoteController {

    private final QuoteAdminService quoteAdminService;
    private final QuoteSubmissionService quoteSubmissionService;

    public AdminQuoteController(QuoteAdminService quoteAdminService,
            QuoteSubmissionService quoteSubmissionService) {
        this.quoteAdminService = quoteAdminService;
        this.quoteSubmissionService = quoteSubmissionService;
    }

    @GetMapping
    @Operation(summary = "List quotes",
            description = "Newest first. All filters are optional and combine with AND.")
    public PageResponse<QuoteSummary> list(
            @RequestParam(name = "orgSlug", required = false) String organizationSlug,
            @RequestParam(required = false) QuoteStatus status,
            @RequestParam(required = false) String clientEmail,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return quoteAdminService.list(organizationSlug, status, clientEmail, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one quote in full, including line items and picture keys")
    public QuoteResponse get(@PathVariable Long id) {
        return quoteAdminService.get(id);
    }

    @PostMapping
    @Operation(summary = "Record a quote taken by phone or in person",
            description = "Unlike the public funnel this sends no email: the client never "
                    + "asked for a confirmation, and the business does not need notifying "
                    + "about a lead it just entered itself.")
    public ResponseEntity<QuoteResponse> create(@Valid @RequestBody AdminQuoteCreateRequest request) {
        QuoteResponse created = quoteSubmissionService
                .createForStaff(request.organizationSlug(), request.quote());
        return ResponseEntity.created(URI.create("/api/admin/quotes/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update status, description or expiry",
            description = "Omitted or null fields are left unchanged.")
    public QuoteResponse update(@PathVariable Long id, @Valid @RequestBody QuoteUpdateRequest request) {
        return quoteAdminService.update(id, request);
    }
}
