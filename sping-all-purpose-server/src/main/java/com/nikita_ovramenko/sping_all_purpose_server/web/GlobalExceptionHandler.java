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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserAlreadyExistsException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserNotVerifiedException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.UserWrongPasswordException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.VerificationTokenExpiredException;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.exception.VerificationTokenInvalidException;
import com.nikita_ovramenko.sping_all_purpose_server.common.exception.BadRequestException;
import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ConflictException;
import com.nikita_ovramenko.sping_all_purpose_server.common.exception.ResourceNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.location.exception.InvalidCountryException;
import com.nikita_ovramenko.sping_all_purpose_server.organization.exception.OrganizationNotFoundException;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.exception.ServiceNotOfferedException;
import com.nikita_ovramenko.sping_all_purpose_server.serviceoffering.exception.UnknownServiceException;

/** Maps domain and validation failures onto RFC 9457 problem responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ProblemDetail onInvalidSession(io.jsonwebtoken.JwtException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Session expired or invalid. Please sign in again.");
        problem.setTitle("Authentication failed");
        return problem;
    }

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

    /**
     * Every "no such row" failure, via the shared base type -- QuoteNotFoundException,
     * JobNotFoundException and the rest all extend it, so adding a resource needs no new
     * handler here.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail onResourceNotFound(ResourceNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Not found");
        return problem;
    }

    /**
     * A rule the caller broke that the database either cannot express or would report
     * too vaguely: a second job for one quote, demoting the last admin, adding a service
     * already on the quote.
     */
    /**
     * Malformed in a way Bean Validation cannot express -- typically a combination of
     * fields that is individually valid but contradictory, such as a job creation
     * request carrying both a quoteId and its own client.
     */
    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail onBadRequest(BadRequestException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail onConflict(ConflictException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Conflict");
        return problem;
    }

    /**
     * A query parameter that will not convert -- ?status=NOPE, ?page=abc. This arrives
     * as a type mismatch rather than a validation error, so without this handler it is
     * an unmapped 500 rather than the 400 it plainly is.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail onTypeMismatch(MethodArgumentTypeMismatchException e) {
        Class<?> required = e.getRequiredType();
        String detail = "Parameter '" + e.getName() + "' has an invalid value";
        if (required != null && required.isEnum()) {
            detail += ". Expected one of: " + String.join(", ",
                    java.util.Arrays.stream(required.getEnumConstants()).map(Object::toString).toList());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid parameter");
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
    /**
     * Bad credentials are 401 and deliberately vague: distinguishing "no such user" from
     * "wrong password" tells an attacker which emails have accounts.
     */
    @ExceptionHandler({ UserNotFoundException.class, UserWrongPasswordException.class })
    public ProblemDetail onFailedLogin(RuntimeException e) {
        log.warn("Failed login attempt: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        problem.setTitle("Authentication failed");
        return problem;
    }

    /** 403, not 401: the credentials were right, the account just is not usable yet. */
    @ExceptionHandler(UserNotVerifiedException.class)
    public ProblemDetail onUnverifiedUser(UserNotVerifiedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        problem.setTitle("Account not verified");
        return problem;
    }

    @ExceptionHandler(VerificationTokenInvalidException.class)
    public ProblemDetail onInvalidVerificationToken(VerificationTokenInvalidException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid verification link");
        return problem;
    }

    /** 410 rather than 400: the link was genuine, it is just no longer usable. */
    @ExceptionHandler(VerificationTokenExpiredException.class)
    public ProblemDetail onExpiredVerificationToken(VerificationTokenExpiredException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, e.getMessage());
        problem.setTitle("Verification link expired");
        return problem;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail onDuplicateUser(UserAlreadyExistsException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("User already exists");
        return problem;
    }

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
