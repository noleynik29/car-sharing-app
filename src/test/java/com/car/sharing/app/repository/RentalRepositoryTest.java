package com.car.sharing.app.repository;

import static com.car.sharing.app.util.TestConstants.CLEANUP_RENTALS_PATH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.car.sharing.app.config.CustomMySQLContainer;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.repository.rental.RentalRepository;
import com.car.sharing.app.util.TestUtil;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = CLEANUP_RENTALS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RentalRepositoryTest {

    static {
        CustomMySQLContainer container = CustomMySQLContainer.getInstance();
        container.start();
    }

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User persistUser() {
        return entityManager.persistAndFlush(TestUtil.createUser());
    }

    private Car persistCar() {
        return entityManager.persistAndFlush(TestUtil.createCar());
    }

    @Test
    @DisplayName("Should find active rentals (not yet returned) by user id")
    void shouldFindByUserIdAndActualReturnDateIsNull() {
        User user = persistUser();
        Car car = persistCar();
        rentalRepository.save(TestUtil.createRental(car, user,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), null));
        rentalRepository.save(TestUtil.createRental(car, user,
                LocalDate.now().minusDays(5), LocalDate.now().minusDays(2), LocalDate.now().minusDays(2)));

        Page<Rental> result = rentalRepository.findByUserIdAndActualReturnDateIsNull(
                user.getId(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Should find returned rentals by user id")
    void shouldFindByUserIdAndActualReturnDateIsNotNull() {
        User user = persistUser();
        Car car = persistCar();
        rentalRepository.save(TestUtil.createRental(car, user,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), null));
        rentalRepository.save(TestUtil.createRental(car, user,
                LocalDate.now().minusDays(5), LocalDate.now().minusDays(2), LocalDate.now().minusDays(2)));

        Page<Rental> result = rentalRepository.findByUserIdAndActualReturnDateIsNotNull(
                user.getId(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Should find rentals by user id with car and user eagerly fetched")
    void shouldFindByUserId_withEntityGraph() {
        User user = persistUser();
        Car car = persistCar();
        rentalRepository.save(TestUtil.createRental(car, user,
                LocalDate.now(), LocalDate.now().plusDays(3), null));

        entityManager.flush();
        entityManager.clear();

        Page<Rental> result = rentalRepository.findByUserId(user.getId(), PageRequest.of(0, 10));
        Rental fetched = result.getContent().get(0);

        assertDoesNotThrow(() -> {
            fetched.getCar().getBrand();
            fetched.getUser().getEmail();
        });
    }

    @Test
    @DisplayName("Should find rental by id and user id with car and user eagerly fetched")
    void shouldFindByIdAndUserId() {
        User user = persistUser();
        Car car = persistCar();
        Rental saved = rentalRepository.save(TestUtil.createRental(car, user,
                LocalDate.now(), LocalDate.now().plusDays(3), null));

        entityManager.flush();
        entityManager.clear();

        Rental actual = rentalRepository.findByIdAndUserId(saved.getId(), user.getId()).orElseThrow();

        assertEquals(saved.getId(), actual.getId());
        assertDoesNotThrow(() -> actual.getCar().getBrand());
    }

    @Test
    @DisplayName("Should return empty when rental id and user id do not match")
    void shouldReturnEmpty_WhenIdAndUserIdMismatch() {
        User user = persistUser();
        User otherUser = entityManager.persistAndFlush(TestUtil.createUser());
        otherUser.setEmail("other@example.com");
        Car car = persistCar();
        Rental saved = rentalRepository.save(TestUtil.createRental(car, user,
                LocalDate.now(), LocalDate.now().plusDays(3), null));

        boolean found = rentalRepository.findByIdAndUserId(saved.getId(), otherUser.getId()).isPresent();

        assertEquals(false, found);
    }

    @Test
    @DisplayName("Should find overdue rentals (return date passed, not yet returned)")
    void shouldFindByReturnDateBeforeAndActualReturnDateIsNull() {
        User user = persistUser();
        Car car = persistCar();
        rentalRepository.save(TestUtil.createRental(car, user,
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(3), null)); // overdue
        rentalRepository.save(TestUtil.createRental(car, user,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), null)); // not yet due
        rentalRepository.save(TestUtil.createRental(car, user,
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(3), LocalDate.now().minusDays(2))); // already returned

        List<Rental> overdue = rentalRepository.findByReturnDateBeforeAndActualReturnDateIsNull(LocalDate.now());

        assertEquals(1, overdue.size());
        assertTrue(overdue.get(0).getReturnDate().isBefore(LocalDate.now()));
    }

    @Test
    @DisplayName("Should throw exception when saving rental with null required fields")
    void shouldThrowException_WhenSavingInvalidRental() {
        Rental rental = new Rental();

        assertThrows(Exception.class, () ->
                rentalRepository.save(rental)
        );
    }
}
