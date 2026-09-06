package com.nikita_ovramenko.sping_all_purpose_server.common.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * A stable page envelope.
 *
 * <p>Spring's own {@code Page}/{@code PageImpl} is deliberately not returned from a
 * controller: its JSON shape is documented as unstable, Boot logs a warning when one is
 * serialized, and springdoc renders it as a wall of internal Pageable/Sort fields. Five
 * named fields say everything a client needs.
 *
 * @param content  the rows on this page
 * @param page     zero-based page index
 * @param size     requested page size
 * @param totalElements total matching rows across all pages
 * @param totalPages    total number of pages
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    /** Maps a page of entities to a page of DTOs, preserving the paging metadata. */
    public static <E, D> PageResponse<D> of(Page<E> source, Function<E, D> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages());
    }
}
