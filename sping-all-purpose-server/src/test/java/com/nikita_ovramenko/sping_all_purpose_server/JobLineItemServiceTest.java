package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.nikita_ovramenko.sping_all_purpose_server.job.model.Job;
import com.nikita_ovramenko.sping_all_purpose_server.job.repository.JobRepo;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto.JobLineItemCreateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.dto.JobLineItemUpdateRequest;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.enums.JobServiceStatus;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.exception.JobLineItemNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.model.JobLineItem;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.repository.JobLineItemRepo;
import com.nikita_ovramenko.sping_all_purpose_server.joblineitem.service.JobLineItemService;
import com.nikita_ovramenko.sping_all_purpose_server.organization.model.Organization;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.service.OfferedServiceResolver;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.model.ServiceOffering;

class JobLineItemServiceTest {
    private final JobRepo jobs = mock(JobRepo.class);
    private final JobLineItemRepo items = mock(JobLineItemRepo.class);
    private final OfferedServiceResolver offerings = mock(OfferedServiceResolver.class);
    private final JobLineItemService service = new JobLineItemService(jobs, items, offerings);
    private final Job job = new Job();
    private final JobLineItem item = new JobLineItem();

    @BeforeEach
    void setUp() {
        job.setId(7L);
        job.setOrganization(new Organization());
        ServiceOffering roofing = new ServiceOffering();
        roofing.setId(11L);
        item.setJob(job);
        item.setService(roofing);
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.TEN);
        item.setStatus(JobServiceStatus.PENDING);
        when(items.findById(42L)).thenReturn(Optional.of(item));
        when(jobs.findById(7L)).thenReturn(Optional.of(job));
        when(offerings.requireOffered(job.getOrganization(), 11L)).thenReturn(roofing);
        when(items.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void wrongParentCannotEditOrDeleteALine() {
        assertThatThrownBy(() -> service.update(1L, 42L,
                new JobLineItemUpdateRequest(BigDecimal.ONE, null, null, null)))
                .isInstanceOf(JobLineItemNotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L, 42L))
                .isInstanceOf(JobLineItemNotFoundException.class);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("10");
        verify(items, never()).save(any());
        verify(items, never()).delete(any(JobLineItem.class));
    }

    @Test
    void aStatusPatchKeepsTheExistingPriceAndQuantity() {
        var response = service.update(7L, 42L,
                new JobLineItemUpdateRequest(null, null, null, JobServiceStatus.COMPLETED));
        assertThat(response.status()).isEqualTo(JobServiceStatus.COMPLETED);
        assertThat(response.lineTotal()).isEqualByComparingTo("20");
    }

    @Test
    void repeatedServicesAreAllowedAndDefaultToPending() {
        var request = new JobLineItemCreateRequest(11L, 1, BigDecimal.TEN, null, null);
        assertThat(service.add(7L, request).status()).isEqualTo(JobServiceStatus.PENDING);
        assertThat(service.add(7L, request).status()).isEqualTo(JobServiceStatus.PENDING);
        verify(items, times(2)).save(any(JobLineItem.class));
    }
}
