package com.nikita_ovramenko.sping_all_purpose_server.quote.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;

@Repository
public interface QuoteRepo extends JpaRepository<Quote, Long> {

    /**
     * Loads the associations the confirmation emails need in one query.
     *
     * <p>pictures is deliberately absent from the graph: fetching it alongside items
     * -- two Lists -- throws MultipleBagFetchException. It lazy-loads in the same
     * transaction instead, costing one extra query.
     */
    @EntityGraph(attributePaths = { "client", "organization", "location", "items", "items.service" })
    Optional<Quote> findWithDetailsById(Long id);

    List<Quote> findByClientId(Long clientId);

    List<Quote> findByOrganizationId(Long organizationId);
}
