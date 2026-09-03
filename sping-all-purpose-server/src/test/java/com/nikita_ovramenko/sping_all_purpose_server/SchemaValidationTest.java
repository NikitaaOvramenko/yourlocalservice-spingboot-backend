package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.nikita_ovramenko.sping_all_purpose_server.organization.repository.OrganizationRepo;
import com.nikita_ovramenko.sping_all_purpose_server.organizationserviceoffering.repository.OrganizationServiceOfferingRepo;

/**
 * The gate for the Flyway baseline.
 *
 * <p>Boots the whole application against a real Postgres with Flyway enabled and
 * ddl-auto=validate. If it passes, V1 and all ten JPA mappings agree -- every column
 * type mismatch, missing table, wrong varchar length and timestamp-vs-timestamptz
 * error surfaces here rather than at deploy time.
 */
@SpringBootTest
@ActiveProfiles("test")
class SchemaValidationTest extends AbstractPostgresTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrganizationRepo organizationRepo;

    @Autowired
    private OrganizationServiceOfferingRepo organizationServiceOfferingRepo;

    @Test
    void hibernateValidatesAgainstTheFlywayBaseline() {
        // Reaching this point at all means validate passed during context startup.
        Integer tables = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public'",
                Integer.class);
        assertThat(tables).isNotNull();
    }

    @Test
    void bothMigrationsApplied() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class);
        assertThat(versions).containsExactly("1", "2", "3");
    }

    @Test
    void allFourSitesAreSeeded() {
        assertThat(organizationRepo.findBySlugIgnoreCase("yourlocalpaints")).isPresent();
        assertThat(organizationRepo.findBySlugIgnoreCase("yourlocalhandyman")).isPresent();
        assertThat(organizationRepo.findBySlugIgnoreCase("tcs")).isPresent();
        assertThat(organizationRepo.findBySlugIgnoreCase("yourlocaljunkremoval")).isPresent();
        assertThat(organizationRepo.count()).isEqualTo(4);
    }

    /** TCS is on its own domain, so it must not inherit the shared inbox. */
    @Test
    void contactEmailsRouteToTheRightInbox() {
        assertThat(organizationRepo.findBySlugIgnoreCase("tcs").orElseThrow().getContactEmail())
                .isEqualTo("tcs.ontario@gmail.com");
        assertThat(organizationRepo.findBySlugIgnoreCase("yourlocalpaints").orElseThrow().getContactEmail())
                .isEqualTo("info@yourlocalservice.co");
    }

    @Test
    void seedCatalogMatchesEachSiteQuoteForm() {
        assertThat(catalogSlugsFor("yourlocalpaints")).containsExactlyInAnyOrder(
                "interior-painting", "exterior-painting",
                "deck-fence-staining-painting", "cabinet-painting-refinishing");

        assertThat(catalogSlugsFor("yourlocalhandyman")).containsExactlyInAnyOrder(
                "general-repairs", "appliance-repair", "junk-removal",
                "carpentry-furniture-assembly", "minor-plumbing-fixes", "drywall-wall-patching");

        assertThat(catalogSlugsFor("tcs")).hasSize(10)
                .contains("repair-and-insulation-of-roofs", "thermal-imaging-survey", "gazebo");

        assertThat(catalogSlugsFor("yourlocaljunkremoval")).containsExactlyInAnyOrder(
                "furniture-removal", "property-cleanouts", "appliance-removal", "waste-removal");
    }

    /**
     * The catalog is global, so a service must not leak into an org that does not sell
     * it -- Handyman's "Junk Removal" and JunkRemoval's own list are separate things.
     */
    @Test
    void catalogIsScopedPerOrganization() {
        assertThat(catalogSlugsFor("yourlocaljunkremoval")).doesNotContain("junk-removal");
        assertThat(catalogSlugsFor("yourlocalpaints")).doesNotContain("deck-and-fences");
        assertThat(catalogSlugsFor("tcs")).doesNotContain("deck-fence-staining-painting");
    }

    private List<String> catalogSlugsFor(String slug) {
        return organizationServiceOfferingRepo.findActiveServicesByOrganizationSlug(slug).stream()
                .map(s -> s.getSlug())
                .toList();
    }

    /**
     * V3 ships every org unconfigured, so they all keep using the global spring.mail.*
     * sender and this migration changes no behaviour on its own.
     */
    @Test
    void orgsShipWithNoOwnMailSettings() {
        assertThat(organizationRepo.findBySlugIgnoreCase("tcs").orElseThrow().getMailSettings())
                .satisfiesAnyOf(
                        ms -> assertThat(ms).isNull(),
                        ms -> assertThat(ms.isConfigured()).isFalse());
    }

    /** Half-configured mail would only fail at send time, after the quote is committed. */
    @Test
    void partialMailSettingsAreRejectedByTheDatabase() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update organization set smtp_host = 'smtp.gmail.com' where slug = 'tcs'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * The identity sequences must have been advanced past the explicitly-seeded ids,
     * or the first application insert collides on the primary key.
     */
    @Test
    void identitySequencesWereResyncedAfterSeeding() {
        Long nextOrgId = jdbcTemplate.queryForObject(
                "select nextval(pg_get_serial_sequence('organization', 'id'))", Long.class);
        assertThat(nextOrgId).isGreaterThan(3L);

        Long nextServiceId = jdbcTemplate.queryForObject(
                "select nextval(pg_get_serial_sequence('service', 'id'))", Long.class);
        assertThat(nextServiceId).isGreaterThan(24L);
    }
}
