package com.nikita_ovramenko.sping_all_purpose_server.quote.service;

import org.springframework.data.jpa.domain.Specification;

import com.nikita_ovramenko.sping_all_purpose_server.quote.enums.QuoteStatus;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;

import jakarta.persistence.criteria.JoinType;

/** Filters for the admin quote list. Each returns null when its filter is absent. */
public final class QuoteSpecifications {

    private QuoteSpecifications() {
    }

    public static Specification<Quote> organizationSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("organization").get("slug")), slug.toLowerCase());
    }

    public static Specification<Quote> status(QuoteStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Quote> clientEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(
                cb.lower(root.get("client").get("email")), email.toLowerCase());
    }

    /**
     * Loads client and location alongside the quote, so building a page of summaries is
     * one query rather than one per row.
     *
     * <p>The result-type guard is load-bearing: Page runs a count query against the same
     * specification, and a count query cannot carry a fetch join -- without the check,
     * paging fails outright.
     */
    public static Specification<Quote> fetchSummaryAssociations() {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("client", JoinType.LEFT);
                root.fetch("location", JoinType.LEFT);
                root.fetch("organization", JoinType.LEFT);
            }
            return null;
        };
    }
}
