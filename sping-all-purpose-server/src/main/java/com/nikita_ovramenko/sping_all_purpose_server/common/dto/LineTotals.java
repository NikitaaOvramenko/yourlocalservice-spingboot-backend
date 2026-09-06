package com.nikita_ovramenko.sping_all_purpose_server.common.dto;

import java.math.BigDecimal;

/**
 * Line-item aggregates for a page of parents, loaded in one grouped query.
 *
 * <p>Both Quote and Job keep their line items lazy, and neither collection can be
 * fetch-joined into a list query -- Quote also has the pictures collection, and
 * Hibernate refuses to fetch two collections at once. Touching getItems() per row would
 * be an N+1, so both list endpoints load their totals through this projection instead.
 *
 * <p>{@code total} is null when nothing on the parent has been priced: SQL SUM ignores
 * nulls and returns null over an all-null set, which is the meaning wanted.
 */
public interface LineTotals {

    /** The parent's id -- a quote id or a job id, depending on the query. */
    Long getOwnerId();

    long getItemCount();

    BigDecimal getTotal();
}
