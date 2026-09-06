package com.nikita_ovramenko.sping_all_purpose_server.job.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.nikita_ovramenko.sping_all_purpose_server.client.dto.ClientSummary;
import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.job.dto.JobResponse;
import com.nikita_ovramenko.sping_all_purpose_server.job.model.Job;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto.JobLineItemResponse;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.model.JobLineItem;
import com.nikita_ovramenko.sping_all_purpose_server.location.dto.LocationSummary;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;

/**
 * Job -> API response.
 *
 * <p>Like QuoteMapper there is no toEntity counterpart: a valid Job needs a resolved
 * Organization, Client and Location, none of which a request DTO carries.
 *
 * <p>Must be called inside an open transaction -- every association it reads is lazy and
 * open-in-view is off.
 */
@Component
public class JobMapper {

    public JobResponse toResponse(Job job) {
        Client client = job.getClient();
        Location location = job.getLocation();

        List<JobLineItemResponse> services = job.getItems().stream()
                .map(JobMapper::toServiceResponse)
                .toList();

        return new JobResponse(
                job.getId(),
                // Reading the id off the lazy proxy does not initialise it: quote_id is
                // the job's own foreign key column.
                job.getQuote() == null ? null : job.getQuote().getId(),
                job.getOrganization().getSlug(),
                job.getStatus(),
                job.getScheduledAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                new ClientSummary(client.getId(), client.getFirstName(), client.getLastName(),
                        client.getEmail(), client.getPhone()),
                new LocationSummary(location.getId(), location.getCountry(), location.getProvinceState(),
                        location.getCity(), location.getStreet(), location.getPostalCode()),
                services,
                job.getDescription(),
                total(job.getItems()));
    }

    public static JobLineItemResponse toServiceResponse(JobLineItem item) {
        return new JobLineItemResponse(
                item.getId(),
                item.getService().getId(),
                item.getService().getName(),
                item.getService().getSlug(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.lineTotal(),
                item.getDescription(),
                item.getStatus());
    }

    /** Null rather than zero when nothing is priced, matching the list endpoint. */
    private static BigDecimal total(List<JobLineItem> items) {
        BigDecimal sum = null;
        for (JobLineItem item : items) {
            BigDecimal line = item.lineTotal();
            if (line != null) {
                sum = sum == null ? line : sum.add(line);
            }
        }
        return sum;
    }
}
