package com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemResponse;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.dto.QuoteLineItemUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.quotelineitem.service.QuoteLineItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * The lines of a quote, which is where prices are actually set.
 *
 * <p>Nested under the quote because a line item has no meaning apart from it, and
 * because nesting makes the ownership check structural: every handler passes both ids
 * down, and the service refuses an item belonging to a different quote.
 */
@RestController
@RequestMapping("/api/admin/quotes/{quoteId}/items")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: quote line items", description = "Add, price and remove the lines of a quote")
public class AdminQuoteLineItemController {

    private final QuoteLineItemService lineItemService;

    public AdminQuoteLineItemController(QuoteLineItemService lineItemService) {
        this.lineItemService = lineItemService;
    }

    @GetMapping
    @Operation(summary = "List the lines on a quote")
    public List<QuoteLineItemResponse> list(@PathVariable Long quoteId) {
        return lineItemService.list(quoteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a service to the quote",
            description = "422 if the organization does not offer that service; "
                    + "409 if it is already on the quote.")
    public QuoteLineItemResponse add(@PathVariable Long quoteId,
            @Valid @RequestBody QuoteLineItemCreateRequest request) {
        return lineItemService.add(quoteId, request);
    }

    @PatchMapping("/{itemId}")
    @Operation(summary = "Set the price, quantity or description of a line",
            description = "Omitted or null fields are left unchanged.")
    public QuoteLineItemResponse update(@PathVariable Long quoteId, @PathVariable Long itemId,
            @Valid @RequestBody QuoteLineItemUpdateRequest request) {
        return lineItemService.update(quoteId, itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a line from the quote")
    public void delete(@PathVariable Long quoteId, @PathVariable Long itemId) {
        lineItemService.delete(quoteId, itemId);
    }
}
