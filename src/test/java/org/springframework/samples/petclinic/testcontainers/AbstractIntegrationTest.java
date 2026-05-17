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

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for Spring Boot integration tests that require a real PostgreSQL database.
 * <p>
 * Each concrete test class must declare its own static {@code @Container} and
 * {@code @DynamicPropertySource} so Testcontainers and Spring do not share one JVM-wide
 * container or a cached {@code ApplicationContext} tied to a stopped instance.
 */
public abstract class AbstractIntegrationTest {

	protected static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

	/**
	 * Registers datasource properties for the given container. Call from a
	 * {@code @DynamicPropertySource} method in the concrete test class that owns the
	 * {@code @Container} field.
	 */
	protected static void registerPostgresProperties(DynamicPropertyRegistry registry, PostgreSQLContainer container) {
		registry.add("spring.datasource.url", container::getJdbcUrl);
		registry.add("spring.datasource.username", container::getUsername);
		registry.add("spring.datasource.password", container::getPassword);
		registry.add("database", () -> "postgres");
		registry.add("spring.docker.compose.enabled", () -> "false");
	}

}
