package com.nikita_ovramenko.sping_all_purpose_server.joblineitem.exception;

import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ResourceNotFoundException;

public class JobLineItemNotFoundException extends ResourceNotFoundException {

    /**
     * Says "on job N" rather than naming the item alone: the same message is used when
     * the item exists but belongs to another job, and reporting that as not found is
     * what stops /jobs/1/items/{itemId} reaching into another job's lines.
     */
    public JobLineItemNotFoundException(Long jobId, Long itemId) {
        super("No line item with id " + itemId + " on job " + jobId);
    }
}
