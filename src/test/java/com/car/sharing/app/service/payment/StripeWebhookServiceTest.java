package com.car.sharing.app.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.exception.EntityNotFoundException;
import com.car.sharing.app.repository.payment.PaymentRepository;
import com.car.sharing.app.service.notification.NotificationService;
import com.car.sharing.app.service.payment.impl.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StripeWebhookService stripeWebhookService;

    private Payment payment;
    private Session stripeSession;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stripeWebhookService, "endpointSecret", "whsec_test_secret");

        Car car = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Camry")
                .type(Car.Type.SEDAN)
                .inventory(5)
                .dailyFee(BigDecimal.valueOf(45.50))
                .build();

        User customer = User.builder()
                .id(1L)
                .email("customer@example.com")
                .role(User.Role.CUSTOMER)
                .build();

        Rental rental = Rental.builder()
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

        stripeSession = new Session();
        stripeSession.setId("cs_test_123");
    }

    private Event mockEvent(String eventType, Session session) {
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(event.getType()).thenReturn(eventType);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        return event;
    }

    @Test
    @DisplayName("Should mark payment as PAID on checkout.session.completed event")
    void handleWebhook_SessionCompleted_MarksPaymentAsPaid() {
        Event event = mockEvent("checkout.session.completed", stripeSession);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test_secret"))
                    .thenReturn(event);
            when(paymentRepository.findBySessionId("cs_test_123"))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.save(payment)).thenReturn(payment);

            stripeWebhookService.handleWebhook("payload", "sig");

            assertEquals(Payment.Status.PAID, payment.getStatus());
            verify(paymentRepository).save(payment);
        }
    }

    @Test
    @DisplayName("Should send payment succeeded notification on checkout.session.completed")
    void handleWebhook_SessionCompleted_SendsPaymentSucceededNotification() {
        Event event = mockEvent("checkout.session.completed", stripeSession);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test_secret"))
                    .thenReturn(event);
            when(paymentRepository.findBySessionId("cs_test_123"))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.save(payment)).thenReturn(payment);

            stripeWebhookService.handleWebhook("payload", "sig");

            verify(notificationService).sendPaymentSucceededMessage(payment);
        }
    }

    @Test
    @DisplayName("Should mark payment as EXPIRED on checkout.session.expired event")
    void handleWebhook_SessionExpired_MarksPaymentAsExpired() {
        Event event = mockEvent("checkout.session.expired", stripeSession);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test_secret"))
                    .thenReturn(event);
            when(paymentRepository.findBySessionId("cs_test_123"))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.save(payment)).thenReturn(payment);

            stripeWebhookService.handleWebhook("payload", "sig");

            assertEquals(Payment.Status.EXPIRED, payment.getStatus());
            verify(paymentRepository).save(payment);
        }
    }

    @Test
    @DisplayName("Should not send notification on checkout.session.expired event")
    void handleWebhook_SessionExpired_SendsNoNotification() {
        Event event = mockEvent("checkout.session.expired", stripeSession);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test_secret"))
                    .thenReturn(event);
            when(paymentRepository.findBySessionId("cs_test_123"))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.save(payment)).thenReturn(payment);

            stripeWebhookService.handleWebhook("payload", "sig");

            verify(notificationService, never()).sendPaymentSucceededMessage(any());
        }
    }

    @Test
    @DisplayName("Should not update payment on unhandled event type")
    void handleWebhook_UnhandledEventType_DoesNotUpdatePayment() {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn("payment_intent.created");

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test_secret"))
                    .thenReturn(event);

            stripeWebhookService.handleWebhook("payload", "sig");

            verify(paymentRepository, never()).findBySessionId(any());
            verify(paymentRepository, never()).save(any());
            verify(notificationService, never()).sendPaymentSucceededMessage(any());
        }
    }

    @Test
    @DisplayName("Should silently swallow SignatureVerificationException")
    void handleWebhook_InvalidSignature_DoesNotThrow() {
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "bad_sig", "whsec_test_secret"))
                    .thenThrow(new SignatureVerificationException("Invalid signature", "bad_sig"));

            // must NOT propagate — the service catches and logs it
            stripeWebhookService.handleWebhook("payload", "bad_sig");

            verify(paymentRepository, never()).findBySessionId(any());
            verify(paymentRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Should not save payment when status is already the same")
    void handleWebhook_StatusAlreadySame_DoesNotSave() {
        payment.setStatus(Payment.Status.PAID);
        Event event = mockEvent("checkout.session.completed", stripeSession);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test_secret"))
                    .thenReturn(event);
            when(paymentRepository.findBySessionId("cs_test_123"))
                    .thenReturn(Optional.of(payment));

            stripeWebhookService.handleWebhook("payload", "sig");

            verify(paymentRepository, never()).save(any());
            verify(notificationService, never()).sendPaymentSucceededMessage(any());
        }
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when payment not found for session id")
    void handleWebhook_PaymentNotFound_ThrowsEntityNotFoundException() {
        Event event = mockEvent("checkout.session.completed", stripeSession);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test_secret"))
                    .thenReturn(event);
            when(paymentRepository.findBySessionId("cs_test_123"))
                    .thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> stripeWebhookService.handleWebhook("payload", "sig"));
            verify(paymentRepository, never()).save(any());
        }
    }
}
