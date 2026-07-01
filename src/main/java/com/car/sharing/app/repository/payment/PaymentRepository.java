package com.car.sharing.app.repository.payment;

import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByRentalUserId(Long userId, Pageable pageable);

    Optional<Payment> findBySessionId(String sessionId);

    boolean existsByRentalAndStatus(Rental rental, Payment.Status paymentStatus);

    boolean existsByRentalUserAndStatus(User user, Payment.Status paymentStatus);
}
