/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Minimal Testcontainers smoke test: start PostgreSQL in Docker and verify connectivity.
 * <p>
 * This class does not load the Spring context; it demonstrates Testcontainers lifecycle
 * in isolation.
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgreSQLContainerTest {

	/**
	 * {@code @Testcontainers} activates the JUnit 5 extension that manages Docker
	 * containers declared with {@code @Container}.
	 * <p>
	 * Lifecycle for this field:
	 * <ol>
	 * <li>Before the first test: Testcontainers pulls the image (if needed) and runs
	 * {@code docker run} for PostgreSQL.</li>
	 * <li>During tests: the container stays running; JDBC URL points at the mapped port
	 * on localhost.</li>
	 * <li>After the last test in this class: Testcontainers stops and removes the
	 * container automatically—no manual cleanup in {@code @AfterAll} is required.</li>
	 * </ol>
	 */
	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

	@Test
	void postgreSqlContainerIsRunningAndAcceptsJdbcConnections() throws Exception {
		// Testcontainers has already started the container via the @Container extension.
		assertThat(POSTGRES.isRunning()).isTrue();

		String jdbcUrl = POSTGRES.getJdbcUrl();
		System.out.println("Testcontainers PostgreSQL JDBC URL: " + jdbcUrl);

		// Prove we can talk to the real database process inside Docker, not an in-memory
		// stub.
		try (Connection connection = DriverManager.getConnection(jdbcUrl, POSTGRES.getUsername(),
				POSTGRES.getPassword())) {
			assertThat(connection.isValid(2)).isTrue();
		}
	}

}
