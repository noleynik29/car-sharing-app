package com.car.sharing.app.repository;

import static com.car.sharing.app.util.TestConstants.CLEANUP_PAYMENTS_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.car.sharing.app.config.CustomMySQLContainer;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.repository.payment.PaymentRepository;
import com.car.sharing.app.util.TestUtil;
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
@Sql(scripts = CLEANUP_PAYMENTS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PaymentRepositoryTest {

    static {
        CustomMySQLContainer container = CustomMySQLContainer.getInstance();
        container.start();
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Rental persistRental() {
        Car car = entityManager.persistAndFlush(TestUtil.createCar());
        User user = entityManager.persistAndFlush(TestUtil.createUser());
        return entityManager.persistAndFlush(TestUtil.createRental(car, user));
    }

    @Test
    @DisplayName("Should save payment and return it by id")
    void shouldSavePaymentAndReturnItById() {
        Rental rental = persistRental();
        Payment payment = TestUtil.createPayment(rental, Payment.Status.PENDING);

        Payment saved = paymentRepository.save(payment);
        Payment actual = paymentRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(saved.getId());
        assertEquals(saved.getSessionId(), actual.getSessionId());
        assertEquals(saved.getStatus(), actual.getStatus());
    }

    @Test
    @DisplayName("Should find payments by rental's user id")
    void shouldFindPaymentsByRentalUserId() {
        Rental rental = persistRental();
        Long userId = rental.getUser().getId();
        paymentRepository.save(TestUtil.createPayment(rental, Payment.Status.PAID));
        paymentRepository.save(TestUtil.createPayment(rental, Payment.Status.PENDING));

        Page<Payment> result = paymentRepository.findByRentalUserId(userId, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("Should find payment by session id")
    void shouldFindPaymentBySessionId() {
        Rental rental = persistRental();
        Payment payment = TestUtil.createPayment(rental, Payment.Status.PENDING);
        Payment saved = paymentRepository.save(payment);

        Payment actual = paymentRepository.findBySessionId(saved.getSessionId()).orElseThrow();

        assertEquals(saved.getId(), actual.getId());
    }

    @Test
    @DisplayName("Should return empty when session id does not exist")
    void shouldReturnEmpty_WhenSessionIdNotFound() {
        boolean found = paymentRepository.findBySessionId("non-existent-session-id").isPresent();

        assertFalse(found);
    }

    @Test
    @DisplayName("Should return true when payment exists for rental and status")
    void shouldReturnTrue_WhenPaymentExistsForRentalAndStatus() {
        Rental rental = persistRental();
        paymentRepository.save(TestUtil.createPayment(rental, Payment.Status.PENDING));

        boolean exists = paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PENDING);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when no payment exists for rental and status")
    void shouldReturnFalse_WhenNoPaymentExistsForRentalAndStatus() {
        Rental rental = persistRental();
        paymentRepository.save(TestUtil.createPayment(rental, Payment.Status.PAID));

        boolean exists = paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PENDING);

        assertFalse(exists);
    }

    @Test
    @DisplayName("Should return true when payment exists for rental's user and status")
    void shouldReturnTrue_WhenPaymentExistsForRentalUserAndStatus() {
        Rental rental = persistRental();
        User user = rental.getUser();
        paymentRepository.save(TestUtil.createPayment(rental, Payment.Status.PENDING));

        boolean exists = paymentRepository.existsByRentalUserAndStatus(user, Payment.Status.PENDING);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Should throw exception when saving payment with null fields")
    void shouldThrowException_WhenSavingInvalidPayment() {
        Payment payment = new Payment();

        assertThrows(Exception.class, () ->
                paymentRepository.save(payment)
        );
    }
}
