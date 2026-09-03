package com.nikita_ovramenko.sping_all_purpose_server.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nikita_ovramenko.sping_all_purpose_server.location.exception.InvalidCountryException;
import com.nikita_ovramenko.sping_all_purpose_server.organization.exception.OrganizationNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.exception.ServiceNotOfferedException;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.exception.UnknownServiceException;

/** Maps domain and validation failures onto RFC 9457 problem responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidationFailure(MethodArgumentNotValidException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Invalid request");

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ProblemDetail onOrganizationNotFound(OrganizationNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Organization not found");
        return problem;
    }

    /**
     * 422 rather than 400: the request is well-formed and the services exist, but this
     * organization does not offer them.
     */
    @ExceptionHandler(ServiceNotOfferedException.class)
    public ProblemDetail onServiceNotOffered(ServiceNotOfferedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problem.setTitle("Service not offered by this organization");
        return problem;
    }

    @ExceptionHandler(UnknownServiceException.class)
    public ProblemDetail onUnknownService(UnknownServiceException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Unknown service");
        return problem;
    }

    @ExceptionHandler(InvalidCountryException.class)
    public ProblemDetail onInvalidCountry(InvalidCountryException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid country");
        return problem;
    }

    /**
     * An unparseable enum in the body (e.g. country) arrives here rather than as a
     * validation error; Jackson's default message would expose an internal type name.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail onUnreadableBody(HttpMessageNotReadableException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request body could not be parsed. Check field types and enum values.");
        problem.setTitle("Malformed request body");
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail onDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The request conflicts with existing data.");
        problem.setTitle("Conflict");
        return problem;
    }
}
