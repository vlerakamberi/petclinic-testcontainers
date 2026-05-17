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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Negative tests: verify that database connection failures surface as clear exceptions.
 * <p>
 * Positive tests alone are insufficient for confidence. Negative testing documents how
 * the system behaves when credentials are wrong or the server is unavailable—conditions
 * that occur in misconfiguration, network partitions, or stopped containers.
 */
@Testcontainers(disabledWithoutDocker = true)
class DatabaseConnectionFailureTest {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

	@Test
	void wrongPassword_throwsSqlException() {
		String jdbcUrl = POSTGRES.getJdbcUrl();

		assertThatThrownBy(() -> DriverManager.getConnection(jdbcUrl, POSTGRES.getUsername(), "wrong-password"))
			.isInstanceOf(SQLException.class)
			.satisfies(ex -> assertThat(ex.getMessage()).isNotBlank());
	}

	@Test
	void connectionToStoppedContainer_throwsSqlException() {
		// Use a dedicated container so stopping it does not affect other tests in this
		// class.
		PostgreSQLContainer ephemeral = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
		ephemeral.start();
		String jdbcUrl = ephemeral.getJdbcUrl();
		String username = ephemeral.getUsername();
		String password = ephemeral.getPassword();
		ephemeral.stop();
		assertThat(ephemeral.isRunning()).isFalse();

		assertThatThrownBy(() -> openConnection(jdbcUrl, username, password)).isInstanceOf(SQLException.class);
	}

	private static Connection openConnection(String jdbcUrl, String username, String password) throws SQLException {
		return DriverManager.getConnection(jdbcUrl, username, password);
	}

}
