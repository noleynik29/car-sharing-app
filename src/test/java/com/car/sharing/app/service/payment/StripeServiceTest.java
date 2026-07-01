package com.car.sharing.app.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.service.payment.impl.StripeService;
import com.stripe.exception.ApiException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    private StripeService stripeService;

    private Rental paymentRental;
    private Rental fineRental;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService();
        ReflectionTestUtils.setField(stripeService, "secretKey", "sk_test_fake_key");
        stripeService.init();

        Car car = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Camry")
                .type(Car.Type.SEDAN)
                .inventory(5)
                .dailyFee(BigDecimal.valueOf(45.50))
                .build();

        paymentRental = Rental.builder()
                .id(1L)
                .rentalDate(LocalDate.of(2026, 6, 1))
                .returnDate(LocalDate.of(2026, 6, 5))
                .car(car)
                .build();

        fineRental = Rental.builder()
                .id(2L)
                .rentalDate(LocalDate.of(2026, 6, 1))
                .returnDate(LocalDate.of(2026, 6, 5))
                .actualReturnDate(LocalDate.of(2026, 6, 8))
                .car(car)
                .build();
    }

    @Test
    @DisplayName("Should return session when Stripe creates session successfully for PAYMENT type")
    void createStripeSession_PaymentType_ReturnsSession() throws StripeException {
        Session mockSession = new Session();
        mockSession.setId("cs_test_payment_123");
        mockSession.setUrl("https://checkout.stripe.com/session/test");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            Session result = stripeService.createStripeSession(
                    paymentRental, BigDecimal.valueOf(182.00), Payment.Type.PAYMENT);

            assertNotNull(result);
            assertEquals("cs_test_payment_123", result.getId());
        }
    }

    @Test
    @DisplayName("Should return session when Stripe creates session successfully for FINE type")
    void createStripeSession_FineType_ReturnsSession() throws StripeException {
        Session mockSession = new Session();
        mockSession.setId("cs_test_fine_456");
        mockSession.setUrl("https://checkout.stripe.com/session/fine");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            Session result = stripeService.createStripeSession(
                    fineRental, BigDecimal.valueOf(104.65), Payment.Type.FINE);

            assertNotNull(result);
            assertEquals("cs_test_fine_456", result.getId());
        }
    }

    @Test
    @DisplayName("Should convert amount to cents correctly")
    void createStripeSession_ConvertsAmountToCents() throws StripeException {
        Session mockSession = new Session();
        mockSession.setId("cs_test_cents");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            Session result = stripeService.createStripeSession(
                    paymentRental, BigDecimal.valueOf(45.50), Payment.Type.PAYMENT);

            assertNotNull(result);
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);
        }
    }

    @Test
    @DisplayName("Should throw ArithmeticException when amount has more than 2 decimal places")
    void createStripeSession_AmountWithTooManyDecimals_ThrowsArithmeticException() {
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            assertThrows(ArithmeticException.class, () ->
                    stripeService.createStripeSession(
                            paymentRental,
                            new BigDecimal("45.501"),
                            Payment.Type.PAYMENT));
        }
    }

    @Test
    @DisplayName("Should propagate StripeException when Stripe session creation fails")
    void createStripeSession_StripeThrows_PropagatesStripeException() {
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(new ApiException("Stripe error", "req_123", "api_error", 500, null));

            assertThrows(StripeException.class, () ->
                    stripeService.createStripeSession(
                            paymentRental, BigDecimal.valueOf(182.00), Payment.Type.PAYMENT));
        }
    }

    @Test
    @DisplayName("Should use RENTAL prefix in title for PAYMENT type")
    void createStripeSession_PaymentType_TitleContainsRentalPrefix() throws StripeException {
        Session mockSession = new Session();
        mockSession.setId("cs_test_title");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenAnswer(invocation -> {
                        var params = invocation.getArgument(
                                0, SessionCreateParams.class);
                        String name = params.getLineItems().get(0)
                                .getPriceData()
                                .getProductData()
                                .getName();
                        assertEquals("RENTAL: Toyota Camry", name);
                        return mockSession;
                    });

            stripeService.createStripeSession(
                    paymentRental, BigDecimal.valueOf(182.00), Payment.Type.PAYMENT);
        }
    }

    @Test
    @DisplayName("Should use FINE prefix in title for FINE type")
    void createStripeSession_FineType_TitleContainsFinePrefix() throws StripeException {
        Session mockSession = new Session();
        mockSession.setId("cs_test_fine_title");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenAnswer(invocation -> {
                        var params = invocation.getArgument(
                                0, SessionCreateParams.class);
                        String name = params.getLineItems().get(0)
                                .getPriceData()
                                .getProductData()
                                .getName();
                        assertEquals("FINE: Toyota Camry", name);
                        return mockSession;
                    });

            stripeService.createStripeSession(
                    fineRental, BigDecimal.valueOf(104.65), Payment.Type.FINE);
        }
    }

    @Test
    @DisplayName("Should include rental period in description for PAYMENT type")
    void createStripeSession_PaymentType_DescriptionContainsRentalPeriod() throws StripeException {
        Session mockSession = new Session();
        mockSession.setId("cs_test_desc");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenAnswer(invocation -> {
                        var params = invocation.getArgument(
                                0, SessionCreateParams.class);
                        String description = params.getLineItems().get(0)
                                .getPriceData()
                                .getProductData()
                                .getDescription();
                        assertEquals("Period: 2026-06-01 to 2026-06-05", description);
                        return mockSession;
                    });

            stripeService.createStripeSession(
                    paymentRental, BigDecimal.valueOf(182.00), Payment.Type.PAYMENT);
        }
    }

    @Test
    @DisplayName("Should include overdue days in description for FINE type")
    void createStripeSession_FineType_DescriptionContainsOverdueDays() throws StripeException {
        Session mockSession = new Session();
        mockSession.setId("cs_test_fine_desc");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenAnswer(invocation -> {
                        var params = invocation.getArgument(
                                0, SessionCreateParams.class);
                        String description = params.getLineItems().get(0)
                                .getPriceData()
                                .getProductData()
                                .getDescription();
                        assertEquals(true, description.contains("3 days"));
                        assertEquals(true, description.contains("2026-06-05"));
                        assertEquals(true, description.contains("2026-06-08"));
                        return mockSession;
                    });

            stripeService.createStripeSession(
                    fineRental, BigDecimal.valueOf(104.65), Payment.Type.FINE);
        }
    }

    @Test
    @DisplayName("Should return session when retrieved successfully")
    void getStripeSession_ValidSessionId_ReturnsSession() throws StripeException {
        Session mockSession = new Session();
        mockSession.setId("cs_test_123");
        mockSession.setPaymentStatus("paid");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve("cs_test_123"))
                    .thenReturn(mockSession);

            Session result = stripeService.getStripeSession("cs_test_123");

            assertNotNull(result);
            assertEquals("cs_test_123", result.getId());
            assertEquals("paid", result.getPaymentStatus());
        }
    }

    @Test
    @DisplayName("Should propagate StripeException when session retrieval fails")
    void getStripeSession_StripeThrows_PropagatesStripeException() {
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve("cs_test_invalid"))
                    .thenThrow(new ApiException(
                            "No such session", "req_456", "invalid_request_error", 404, null));

            assertThrows(StripeException.class,
                    () -> stripeService.getStripeSession("cs_test_invalid"));
        }
    }
}
