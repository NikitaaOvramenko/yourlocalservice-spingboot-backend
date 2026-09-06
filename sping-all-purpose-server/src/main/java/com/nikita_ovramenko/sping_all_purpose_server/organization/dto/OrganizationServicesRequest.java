package com.nikita_ovramenko.sping_all_purpose_server.organization.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * Replaces the whole set of services an organization offers.
 *
 * <p>A replace rather than add/remove endpoints: the offering set is small and a client
 * editing it has the full list in front of them, so sending the intended end state
 * avoids a read-modify-write race between two admins.
 *
 * <p>An empty list is allowed and means the organization currently offers nothing --
 * which is what an about-to-launch site looks like.
 */
public record OrganizationServicesRequest(
        @NotNull List<@NotNull Long> serviceIds) {
}
