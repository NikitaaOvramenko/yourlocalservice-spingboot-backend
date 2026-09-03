package com.nikita_ovramenko.sping_all_purpose_server.job.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.common.model.Auditable;
import com.nikita_ovramenko.sping_all_purpose_server.job.enums.JobStatus;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.model.JobLineItem;

/**
 * Work actually performed for a client.
 *
 * <p>client / organization / location are held directly rather than read through the
 * quote. They are job-level business data: a job can be relocated or reassigned after
 * quoting without rewriting the original quote, so they are allowed to diverge.
 */
@Entity
@Table(name = "job", indexes = {
        @Index(name = "ix_job_client", columnList = "client_id"),
        @Index(name = "ix_job_organization", columnList = "organization_id"),
        @Index(name = "ix_job_location", columnList = "location_id"),
        @Index(name = "ix_job_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Job extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nullable so a walk-in job with no prior quote is representable. Uniqueness is
     * enforced by a partial unique index (WHERE quote_id IS NOT NULL), giving the
     * quote 1 -> 0..1 job relationship without blocking walk-ins.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", unique = true)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobLineItem> items = new ArrayList<>();

    public void addItem(JobLineItem item) {
        item.setJob(this);
        this.items.add(item);
    }
}
