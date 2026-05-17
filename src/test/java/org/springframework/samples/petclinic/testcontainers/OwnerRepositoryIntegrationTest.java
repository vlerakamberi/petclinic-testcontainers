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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration tests for {@link OwnerRepository} against a real PostgreSQL instance.
 * <p>
 * A real PostgreSQL Testcontainer is used instead of H2 because production runs on
 * PostgreSQL: SQL dialect, identity columns, constraints, and index behavior differ from
 * H2. Tests that pass on H2 can still fail in production; Testcontainers narrows that gap
 * without requiring a manually installed database.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("postgres")
@Transactional
class OwnerRepositoryIntegrationTest extends AbstractIntegrationTest {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registerPostgresProperties(registry, POSTGRES);
	}

	@Autowired
	private OwnerRepository ownerRepository;

	@Test
	void saveOwner_persistsToPostgreSQL() {
		Owner owner = newOwner("Test", "Containers", "6085551234");

		Owner saved = ownerRepository.saveAndFlush(owner);

		assertThat(saved.getId()).isNotNull();
		assertThat(ownerRepository.findById(saved.getId())).isPresent();
	}

	@Test
	void findByLastNameStartingWith_returnsMatchingOwners() {
		Owner owner = newOwner("Jane", "Testcontainers", "6085559999");
		ownerRepository.saveAndFlush(owner);

		Page<Owner> results = ownerRepository.findByLastNameStartingWith("Test", PageRequest.of(0, 10));

		assertThat(results.getContent()).anyMatch(o -> "Testcontainers".equals(o.getLastName()));
	}

	@Test
	void updateTelephone_persistsChangeInPostgreSQL() {
		Owner owner = newOwner("Update", "Phone", "6085550001");
		Owner saved = ownerRepository.saveAndFlush(owner);

		saved.setTelephone("6085554321");
		ownerRepository.saveAndFlush(saved);

		Owner reloaded = ownerRepository.findById(saved.getId()).orElseThrow();
		assertThat(reloaded.getTelephone()).isEqualTo("6085554321");
	}

	private static Owner newOwner(String firstName, String lastName, String telephone) {
		Owner owner = new Owner();
		owner.setFirstName(firstName);
		owner.setLastName(lastName);
		owner.setAddress("1 Test Street");
		owner.setCity("Madison");
		owner.setTelephone(telephone);
		return owner;
	}

}
