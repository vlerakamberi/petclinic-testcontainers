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

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Integration tests for {@link Pet} persistence through the {@link Owner} aggregate.
 * <p>
 * PetClinic has no separate {@code PetRepository}; pets are persisted via
 * {@link Owner#addPet(Pet)} and {@code CascadeType.ALL} on the owner–pets association.
 * <p>
 * Container lifecycle for <strong>this class only</strong>:
 * <ul>
 * <li><strong>Start:</strong> before the first test method, Testcontainers starts this
 * class's {@code POSTGRES} container.</li>
 * <li><strong>Run:</strong> {@code @DynamicPropertySource} binds Spring to this
 * container's JDBC URL (not another test class's port).</li>
 * <li><strong>Stop:</strong> after the last test method in this class, the container is
 * stopped and removed.</li>
 * </ul>
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("postgres")
@Transactional
class PetRepositoryIntegrationTest extends AbstractIntegrationTest {

	/**
	 * Dedicated container for this test class. Do not reuse a static container from a
	 * superclass—Spring may cache an {@code ApplicationContext} with a stale JDBC URL
	 * after another class's container has stopped.
	 */
	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registerPostgresProperties(registry, POSTGRES);
	}

	@Autowired
	private OwnerRepository ownerRepository;

	@Autowired
	private PetTypeRepository petTypeRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void savePetWithOwner_canBeRetrievedWithNameAndType() {
		PetType catType = petTypeRepository.findPetTypes()
			.stream()
			.filter(type -> "cat".equals(type.getName()))
			.findFirst()
			.orElseThrow();

		Owner owner = newOwner("Pet", "Owner");
		Pet pet = new Pet();
		pet.setName("Fluffy");
		pet.setBirthDate(LocalDate.of(2020, 1, 15));
		pet.setType(catType);
		owner.addPet(pet);

		Owner savedOwner = ownerRepository.saveAndFlush(owner);
		entityManager.clear();

		Owner reloaded = ownerRepository.findById(savedOwner.getId()).orElseThrow();
		Pet reloadedPet = reloaded.getPet("Fluffy");

		assertThat(reloadedPet).isNotNull();
		assertThat(reloadedPet.getName()).isEqualTo("Fluffy");
		assertThat(reloadedPet.getType().getName()).isEqualTo("cat");
	}

	@Test
	void deleteOwner_cascadesToPets() {
		PetType dogType = petTypeRepository.findPetTypes()
			.stream()
			.filter(type -> "dog".equals(type.getName()))
			.findFirst()
			.orElseThrow();

		Owner owner = newOwner("Cascade", "Delete");
		Pet pet = new Pet();
		pet.setName("CascadePet");
		pet.setBirthDate(LocalDate.of(2019, 6, 1));
		pet.setType(dogType);
		owner.addPet(pet);

		Owner savedOwner = ownerRepository.saveAndFlush(owner);
		Integer ownerId = savedOwner.getId();
		Integer petId = savedOwner.getPet("CascadePet").getId();

		ownerRepository.delete(savedOwner);
		ownerRepository.flush();
		entityManager.clear();

		assertThat(ownerRepository.findById(ownerId)).isEmpty();
		assertThat(entityManager.find(Pet.class, petId)).isNull();
	}

	private static Owner newOwner(String firstName, String lastName) {
		Owner owner = new Owner();
		owner.setFirstName(firstName);
		owner.setLastName(lastName);
		owner.setAddress("2 Pet Lane");
		owner.setCity("Madison");
		owner.setTelephone("6085552468");
		return owner;
	}

}
