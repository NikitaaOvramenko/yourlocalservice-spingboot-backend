package com.nikita_ovramenko.sping_all_purpose_server.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables @CreatedDate / @LastModifiedDate population on {@code Auditable} entities.
 *
 * <p>Deliberately a separate @Configuration rather than an annotation on the main
 * application class: on the main class every @WebMvcTest slice would drag in the
 * auditing infrastructure and fail looking for a JpaMetamodelMappingContext.
 *
 * <p>Note that auditing does not fire on bulk JPQL/native UPDATE statements -- any
 * future @Modifying @Query must set updated_at itself.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
