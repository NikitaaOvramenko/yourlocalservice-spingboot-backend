package com.nikita_ovramenko.sping_all_purpose_server.joblineitem.model;

import java.math.BigDecimal;

import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.enums.JobServiceStatus;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.nikita_ovramenko.sping_all_purpose_server.job.model.Job;

/**
 * One service performed as part of a job.
 *
 * <p>Deliberately holds no client / organization / location / quote references --
 * those belong to the owning Job.
 */
@Entity
@Table(name = "job_service", indexes = {
        @Index(name = "ix_job_service_job", columnList = "job_id"),
        @Index(name = "ix_job_service_service", columnList = "service_id")
})
@Getter
@Setter
@NoArgsConstructor
public class JobLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    /** Price per unit, not the line total. Nullable until the org prices the line. */
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobServiceStatus status = JobServiceStatus.PENDING;

    /** Convenience only; the total is computed, never stored. */
    public BigDecimal lineTotal() {
        if (unitPrice == null || quantity == null) {
            return null;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
