package com.nikita_ovramenko.sping_all_purpose_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Now backed by a real Postgres. Previously this could not run at all on a clean
 * machine: DB_URL / SMTP_PASS / the AWS_* variables have no defaults, so placeholder
 * resolution failed at startup and `mvnw verify` was effectively unrunnable.
 */
@SpringBootTest
@ActiveProfiles("test")
class SpingAllPurposeServerApplicationTests extends AbstractPostgresTest {

	@Test
	void contextLoads() {
	}

}
