package com.car.sharing.app.controller;

import static com.car.sharing.app.util.TestConstants.ADD_PENDING_PAYMENT_PATH;
import static com.car.sharing.app.util.TestConstants.ADD_RENTAL_WITH_PAYMENT_PATH;
import static com.car.sharing.app.util.TestConstants.CLEANUP_PAYMENTS_CONTROLLER_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.car.sharing.app.config.CustomMySQLContainer;
import com.car.sharing.app.dto.payment.CreatePaymentRequest;
import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.service.payment.impl.StripeService;
import com.car.sharing.app.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {
    private static final String PAYMENTS_PATH = "/payments";
    private static final String SUCCESS_PATH = "/payments/success";
    private static final String CANCEL_PATH = "/payments/cancel";

    static {
        CustomMySQLContainer container = CustomMySQLContainer.getInstance();
        container.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StripeService stripeService;

    @Test
    @DisplayName("CUSTOMER should get their own payments")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_WITH_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_PENDING_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getPayments_AsCustomer_ReturnsOwnPayments() throws Exception {
        mockMvc.perform(get(PAYMENTS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("MANAGER should get payments for specified userId")
    @WithUserDetails("manager@example.com")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_WITH_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_PENDING_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getPayments_AsManager_ReturnsPaymentsForUser() throws Exception {
        mockMvc.perform(get(PAYMENTS_PATH).param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Anonymous user should be unauthorized to list payments")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getPayments_AsAnonymous_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(PAYMENTS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CUSTOMER should create a payment")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_WITH_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createPayment_AsCustomer_ReturnsCreated() throws Exception {
        Session mockSession = new Session();
        mockSession.setId("cs_test_mock_session_123");
        mockSession.setUrl("https://checkout.stripe.com/session/mock");
        when(stripeService.createStripeSession(
                any(Rental.class), any(BigDecimal.class), eq(Payment.Type.PAYMENT)))
                .thenReturn(mockSession);

        CreatePaymentRequest request = TestUtil.createPaymentRequest(1L);

        mockMvc.perform(post(PAYMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.sessionId").value("cs_test_mock_session_123"));
    }

    @Test
    @DisplayName("Should return bad request when payment already exists for rental")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_WITH_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_PENDING_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createPayment_ActiveSessionExists_ReturnsBadRequest() throws Exception {
        CreatePaymentRequest request = TestUtil.createPaymentRequest(1L);

        mockMvc.perform(post(PAYMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return not found when rental does not exist")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_WITH_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createPayment_NonExistentRental_ReturnsNotFound() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(999L, Payment.Type.PAYMENT);

        mockMvc.perform(post(PAYMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return bad request when request is invalid")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createPayment_NullFields_ReturnsBadRequest() throws Exception {
        String invalidJson = "{\"rentalId\": null, \"paymentType\": null}";

        mockMvc.perform(post(PAYMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("MANAGER should be forbidden from creating a payment")
    @WithUserDetails("manager@example.com")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_WITH_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createPayment_AsManager_ReturnsForbidden() throws Exception {
        CreatePaymentRequest request = TestUtil.createPaymentRequest(1L);

        mockMvc.perform(post(PAYMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should handle Stripe success callback and mark payment as paid")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_WITH_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_PENDING_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void handleSuccess_ValidSession_ReturnsOk() throws Exception {
        Session mockSession = new Session();
        mockSession.setId("cs_test_pending_session_001");
        mockSession.setPaymentStatus("paid");
        when(stripeService.getStripeSession("cs_test_pending_session_001"))
                .thenReturn(mockSession);

        mockMvc.perform(get(SUCCESS_PATH)
                        .param("session_id", "cs_test_pending_session_001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return not found when session id does not match any payment")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void handleSuccess_UnknownSessionId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get(SUCCESS_PATH)
                        .param("session_id", "cs_test_nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle Stripe cancel callback and mark payment as canceled")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_WITH_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_PENDING_PAYMENT_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void handleCancel_ValidSession_ReturnsOk() throws Exception {
        mockMvc.perform(get(CANCEL_PATH)
                        .param("session_id", "cs_test_pending_session_001"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return not found on cancel with unknown session id")
    @Sql(scripts = CLEANUP_PAYMENTS_CONTROLLER_PATH,
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void handleCancel_UnknownSessionId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get(CANCEL_PATH)
                        .param("session_id", "cs_test_nonexistent"))
                .andExpect(status().isNotFound());
    }
}