package com.nikita_ovramenko.sping_all_purpose_server.common.service;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

/**
 * Composes optional filters.
 *
 * <p>Exists because Specification.allOf rejects null elements outright
 * ("Other specification must not be null"), while the natural way to express an absent
 * filter is to return null for it. Filtering first keeps each filter method simple --
 * it either describes a restriction or says there is none.
 */
public final class Specs {

    private Specs() {
    }

    /** Ands together whichever specifications are present, ignoring nulls. */
    @SafeVarargs
    public static <T> Specification<T> allOfNonNull(Specification<T>... specifications) {
        return Specification.allOf(
                Arrays.stream(specifications).filter(Objects::nonNull).toList());
    }
}
