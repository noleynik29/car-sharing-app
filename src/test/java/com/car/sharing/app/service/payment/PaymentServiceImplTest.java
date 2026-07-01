package com.car.sharing.app.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.car.sharing.app.dto.payment.CreatePaymentRequest;
import com.car.sharing.app.dto.payment.PaymentResponse;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.exception.EntityNotFoundException;
import com.car.sharing.app.exception.payment.PaymentProcessingException;
import com.car.sharing.app.mapper.PaymentMapper;
import com.car.sharing.app.repository.payment.PaymentRepository;
import com.car.sharing.app.repository.rental.RentalRepository;
import com.car.sharing.app.service.notification.NotificationService;
import com.car.sharing.app.service.payment.impl.PaymentServiceImpl;
import com.car.sharing.app.service.payment.impl.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private StripeService stripeService;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Car car;
    private User customer;
    private User manager;
    private Rental rental;
    private Payment payment;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        car = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Camry")
                .type(Car.Type.SEDAN)
                .inventory(5)
                .dailyFee(BigDecimal.valueOf(45.50))
                .build();

        customer = User.builder()
                .id(1L)
                .email("customer@example.com")
                .role(User.Role.CUSTOMER)
                .build();

        manager = User.builder()
                .id(2L)
                .email("manager@example.com")
                .role(User.Role.MANAGER)
                .build();

        rental = Rental.builder()
                .id(1L)
                .rentalDate(LocalDate.of(2026, 6, 1))
                .returnDate(LocalDate.of(2026, 6, 5))
                .car(car)
                .user(customer)
                .build();

        payment = Payment.builder()
                .id(1L)
                .status(Payment.Status.PENDING)
                .type(Payment.Type.PAYMENT)
                .rental(rental)
                .sessionUrl("https://checkout.stripe.com/session/test")
                .sessionId("cs_test_123")
                .amountToPay(BigDecimal.valueOf(182.00))
                .build();

        paymentResponse = new PaymentResponse(
                1L,
                Payment.Status.PENDING,
                Payment.Type.PAYMENT,
                1L,
                null,
                "cs_test_123",
                BigDecimal.valueOf(182.00)
        );
    }

    @Test
    @DisplayName("Should create payment and return PaymentResponse")
    void createPayment_ValidRequest_ReturnsPaymentResponse() throws StripeException {
        CreatePaymentRequest request = new CreatePaymentRequest(1L, Payment.Type.PAYMENT);
        Payment mappedPayment = Payment.builder()
                .type(Payment.Type.PAYMENT)
                .rental(rental)
                .build();
        Session mockSession = new Session();
        mockSession.setId("cs_test_123");
        mockSession.setUrl("https://checkout.stripe.com/session/test");

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PENDING))
                .thenReturn(false);
        when(paymentMapper.toModel(request)).thenReturn(mappedPayment);
        when(stripeService.createStripeSession(
                eq(rental), any(BigDecimal.class), eq(Payment.Type.PAYMENT)))
                .thenReturn(mockSession);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponse);

        PaymentResponse actual = paymentService.createPayment(request);

        assertNotNull(actual);
        assertEquals(paymentResponse.sessionId(), actual.sessionId());
        verify(paymentRepository).save(any(Payment.class));
        verify(notificationService).sendCreatePaymentMessage(any(Payment.class));
    }

    @Test
    @DisplayName("Should calculate total rent price for PAYMENT type")
    void createPayment_PaymentType_CalculatesTotalRentPrice() throws StripeException {
        // 4 days * 45.50 = 182.00
        CreatePaymentRequest request = new CreatePaymentRequest(1L, Payment.Type.PAYMENT);
        Payment mappedPayment = Payment.builder().type(Payment.Type.PAYMENT).build();
        Session mockSession = new Session();
        mockSession.setId("cs_test_123");
        mockSession.setUrl("https://checkout.stripe.com/session/test");

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PENDING))
                .thenReturn(false);
        when(paymentMapper.toModel(request)).thenReturn(mappedPayment);
        when(stripeService.createStripeSession(
                eq(rental), eq(BigDecimal.valueOf(182.00)), eq(Payment.Type.PAYMENT)))
                .thenReturn(mockSession);
        when(paymentRepository.save(any())).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponse);

        paymentService.createPayment(request);

        verify(stripeService).createStripeSession(
                eq(rental), eq(BigDecimal.valueOf(182.00)), eq(Payment.Type.PAYMENT));
    }

    @Test
    @DisplayName("Should calculate fine price for FINE type")
    void createPayment_FineType_CalculatesFinePrice() throws StripeException {
        // 2 days overdue * 45.50 * 1.15 = 104.65
        rental.setReturnDate(LocalDate.of(2026, 6, 5));
        rental.setActualReturnDate(LocalDate.of(2026, 6, 7));
        BigDecimal expectedFine = BigDecimal.valueOf(45.50)
                .multiply(BigDecimal.valueOf(2))
                .multiply(BigDecimal.valueOf(1.15));

        CreatePaymentRequest request = new CreatePaymentRequest(1L, Payment.Type.FINE);
        Payment mappedPayment = Payment.builder().type(Payment.Type.FINE).build();
        Session mockSession = new Session();
        mockSession.setId("cs_test_fine");
        mockSession.setUrl("https://checkout.stripe.com/session/fine");

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PENDING))
                .thenReturn(false);
        when(paymentMapper.toModel(request)).thenReturn(mappedPayment);
        when(stripeService.createStripeSession(
                eq(rental), eq(expectedFine), eq(Payment.Type.FINE)))
                .thenReturn(mockSession);
        when(paymentRepository.save(any())).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponse);

        paymentService.createPayment(request);

        verify(stripeService).createStripeSession(eq(rental), eq(expectedFine), eq(Payment.Type.FINE));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when rental not found")
    void createPayment_RentalNotFound_ThrowsEntityNotFoundException() {
        CreatePaymentRequest request = new CreatePaymentRequest(999L, Payment.Type.PAYMENT);
        when(rentalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw PaymentProcessingException when rental is already paid")
    void createPayment_AlreadyPaid_ThrowsPaymentProcessingException() {
        CreatePaymentRequest request = new CreatePaymentRequest(1L, Payment.Type.PAYMENT);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PAID))
                .thenReturn(true);

        assertThrows(PaymentProcessingException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw PaymentProcessingException when payment session is already active")
    void createPayment_PendingSessionExists_ThrowsPaymentProcessingException() {
        CreatePaymentRequest request = new CreatePaymentRequest(1L, Payment.Type.PAYMENT);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PENDING))
                .thenReturn(true);

        assertThrows(PaymentProcessingException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw PaymentProcessingException when FINE requested but no overdue")
    void createPayment_FineWithNoOverdue_ThrowsPaymentProcessingException() {
        // returnDate in future = not overdue yet
        rental.setReturnDate(LocalDate.now().plusDays(5));
        rental.setActualReturnDate(null);
        CreatePaymentRequest request = new CreatePaymentRequest(1L, Payment.Type.FINE);

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PENDING))
                .thenReturn(false);

        assertThrows(PaymentProcessingException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw PaymentProcessingException when Stripe session creation fails")
    void createPayment_StripeThrows_ThrowsPaymentProcessingException() throws StripeException {
        CreatePaymentRequest request = new CreatePaymentRequest(1L, Payment.Type.PAYMENT);
        Payment mappedPayment = Payment.builder().type(Payment.Type.PAYMENT).build();

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.existsByRentalAndStatus(rental, Payment.Status.PENDING))
                .thenReturn(false);
        when(paymentMapper.toModel(request)).thenReturn(mappedPayment);
        when(stripeService.createStripeSession(any(), any(), any()))
                .thenThrow(new StripeException("Stripe error", "req_123", "stripe_error", 500) {});

        assertThrows(PaymentProcessingException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("CUSTOMER should get their own payments using their own id")
    void getPaymentsByUserId_AsCustomer_UsesOwnId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));

        when(paymentRepository.findByRentalUserId(1L, pageable)).thenReturn(paymentPage);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponse);

        Page<PaymentResponse> result = paymentService.getPaymentsByUserId(null, customer, pageable);

        assertEquals(1, result.getTotalElements());
        verify(paymentRepository).findByRentalUserId(1L, pageable);
    }

    @Test
    @DisplayName("MANAGER should get payments using the provided userId")
    void getPaymentsByUserId_AsManager_UsesProvidedUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));

        when(paymentRepository.findByRentalUserId(1L, pageable)).thenReturn(paymentPage);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponse);

        Page<PaymentResponse> result = paymentService.getPaymentsByUserId(1L, manager, pageable);

        assertEquals(1, result.getTotalElements());
        verify(paymentRepository).findByRentalUserId(1L, pageable);
    }

    @Test
    @DisplayName("CUSTOMER should ignore provided userId and use their own")
    void getPaymentsByUserId_AsCustomer_IgnoresProvidedUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findByRentalUserId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(payment)));
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponse);

        paymentService.getPaymentsByUserId(99L, customer, pageable);

        verify(paymentRepository).findByRentalUserId(1L, pageable);
        verify(paymentRepository, never()).findByRentalUserId(eq(99L), any());
    }

    @Test
    @DisplayName("Should mark payment as PAID when Stripe confirms paid status")
    void processSuccess_PaidStatus_MarksPaymentAsPaid() throws StripeException {
        Session mockSession = new Session();
        mockSession.setPaymentStatus("paid");

        when(paymentRepository.findBySessionId("cs_test_123"))
                .thenReturn(Optional.of(payment));
        when(stripeService.getStripeSession("cs_test_123")).thenReturn(mockSession);
        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.processSuccess("cs_test_123");

        assertEquals(Payment.Status.PAID, payment.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Should throw PaymentProcessingException when Stripe status is not paid")
    void processSuccess_NotPaidStatus_ThrowsPaymentProcessingException() throws StripeException {
        Session mockSession = new Session();
        mockSession.setPaymentStatus("unpaid");

        when(paymentRepository.findBySessionId("cs_test_123"))
                .thenReturn(Optional.of(payment));
        when(stripeService.getStripeSession("cs_test_123")).thenReturn(mockSession);

        assertThrows(PaymentProcessingException.class,
                () -> paymentService.processSuccess("cs_test_123"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when session id not found on success")
    void processSuccess_UnknownSessionId_ThrowsEntityNotFoundException() {
        when(paymentRepository.findBySessionId("unknown")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> paymentService.processSuccess("unknown"));
    }

    @Test
    @DisplayName("Should wrap StripeException in PaymentProcessingException on success")
    void processSuccess_StripeThrows_ThrowsPaymentProcessingException() throws StripeException {
        when(paymentRepository.findBySessionId("cs_test_123"))
                .thenReturn(Optional.of(payment));
        when(stripeService.getStripeSession("cs_test_123"))
                .thenThrow(new StripeException("Stripe error", "req_123", "stripe_error", 500) {});

        assertThrows(PaymentProcessingException.class,
                () -> paymentService.processSuccess("cs_test_123"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark payment as CANCELED on cancel")
    void processCancel_ValidSessionId_MarksPaymentAsCanceled() {
        when(paymentRepository.findBySessionId("cs_test_123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.processCancel("cs_test_123");

        assertEquals(Payment.Status.CANCELED, payment.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when session id not found on cancel")
    void processCancel_UnknownSessionId_ThrowsEntityNotFoundException() {
        when(paymentRepository.findBySessionId("unknown")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> paymentService.processCancel("unknown"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not call Stripe on cancel")
    void processCancel_ValidSessionId_DoesNotCallStripe() throws StripeException {
        when(paymentRepository.findBySessionId("cs_test_123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.processCancel("cs_test_123");

        verify(stripeService, never()).getStripeSession(any());
        verify(stripeService, never()).createStripeSession(any(), any(), any());
    }
}
