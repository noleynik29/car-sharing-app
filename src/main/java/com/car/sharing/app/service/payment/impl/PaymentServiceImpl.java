package com.car.sharing.app.service.payment.impl;

import com.car.sharing.app.dto.payment.CreatePaymentRequest;
import com.car.sharing.app.dto.payment.PaymentResponse;
import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.exception.EntityNotFoundException;
import com.car.sharing.app.exception.payment.PaymentProcessingException;
import com.car.sharing.app.mapper.PaymentMapper;
import com.car.sharing.app.repository.payment.PaymentRepository;
import com.car.sharing.app.repository.rental.RentalRepository;
import com.car.sharing.app.service.notification.NotificationService;
import com.car.sharing.app.service.payment.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final BigDecimal FINE_MULTIPLIER = BigDecimal.valueOf(1.15);
    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final StripeService stripeService;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Rental rental = rentalRepository.findById(request.rentalId()).orElseThrow(
                () -> new EntityNotFoundException("Rental by id: "
                        + request.rentalId()
                        + " not found!")
        );

        validatePaymentState(rental, request);

        BigDecimal price = request.paymentType() == Payment.Type.PAYMENT
                ? calculateTotalRentPrice(rental)
                : calculateFinePrice(rental);

        Payment payment = paymentMapper.toModel(request);
        payment.setStatus(Payment.Status.PENDING);
        payment.setAmountToPay(price);
        payment.setRental(rental);

        return processPayment(rental, payment, request);
    }

    @Override
    public Page<PaymentResponse> getPaymentsByUserId(Long userId, User user, Pageable pageable) {
        Long targetUserId = user.getRole() == User.Role.MANAGER
                ? userId
                : user.getId();

        return paymentRepository.findByRentalUserId(targetUserId, pageable)
                .map(paymentMapper::toDto);
    }

    @Override
    public void processSuccess(String sessionId) {
        Payment payment = paymentRepository.findBySessionId(sessionId).orElseThrow(
                () -> new EntityNotFoundException("Can't find payment by id: " + sessionId)
        );

        try {
            Session session = stripeService.getStripeSession(sessionId);
            String paymentStatus = session.getPaymentStatus();

            if ("paid".equals(paymentStatus)) {
                payment.setStatus(Payment.Status.PAID);
                paymentRepository.save(payment);
            } else {
                throw new PaymentProcessingException("Payment not confirmed by Stripe. "
                        + "Current status: " + paymentStatus);
            }
        } catch (StripeException e) {
            throw new PaymentProcessingException(e.getMessage());
        }
    }

    @Override
    public void processCancel(String sessionId) {
        Payment payment = paymentRepository.findBySessionId(sessionId).orElseThrow(
                () -> new EntityNotFoundException("Can't find payment by id: " + sessionId)
        );

        payment.setStatus(Payment.Status.CANCELED);
        paymentRepository.save(payment);
    }

    private BigDecimal calculateTotalRentPrice(Rental rental) {
        long daysRented = ChronoUnit.DAYS.between(rental.getRentalDate(), rental.getReturnDate());
        return rental.getCar().getDailyFee()
                .multiply(BigDecimal.valueOf(daysRented));
    }

    private void validateOverdue(Rental rental) {
        LocalDate endDate = rental.getActualReturnDate() != null
                ? rental.getActualReturnDate()
                : LocalDate.now();

        if (!endDate.isAfter(rental.getReturnDate())) {
            throw new PaymentProcessingException("No overdue");
        }
    }

    private BigDecimal calculateFinePrice(Rental rental) {
        BigDecimal daysOverdue = BigDecimal.valueOf(
                Math.max(0, ChronoUnit.DAYS.between(
                        rental.getReturnDate(),
                        rental.getActualReturnDate()
                ))
        );

        return rental.getCar()
                .getDailyFee()
                .multiply(daysOverdue)
                .multiply(FINE_MULTIPLIER);
    }

    private void validatePaymentState(Rental rental, CreatePaymentRequest request) {
        if (request.paymentType() == Payment.Type.PAYMENT
                && paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PAID)) {
            throw new PaymentProcessingException("Rental by id: " + rental.getId()
                    + " is already paid!");
        }

        if (paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PENDING)) {
            throw new PaymentProcessingException("Payment session is already active!");
        }

        if (request.paymentType() == Payment.Type.FINE) {
            validateOverdue(rental);
        }
    }

    private PaymentResponse processPayment(
            Rental rental,
            Payment payment,
            CreatePaymentRequest request) {
        try {
            Session session = stripeService.createStripeSession(rental,
                    payment.getAmountToPay(),
                    request.paymentType());
            payment.setSessionUrl(session.getUrl());
            payment.setSessionId(session.getId());

            notificationService.sendCreatePaymentMessage(payment);
            return paymentMapper.toDto(paymentRepository.save(payment));
        } catch (StripeException e) {
            throw new PaymentProcessingException("Can't create stripe session: "
                    + e.getMessage());
        }
    }
}
