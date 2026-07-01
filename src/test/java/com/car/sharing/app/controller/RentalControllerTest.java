package com.car.sharing.app.controller;

import static com.car.sharing.app.util.TestConstants.ADD_CAR_WITH_STOCK_PATH;
import static com.car.sharing.app.util.TestConstants.ADD_OUT_OF_STOCK_CAR_PATH;
import static com.car.sharing.app.util.TestConstants.ADD_RENTAL_PATH;
import static com.car.sharing.app.util.TestConstants.ADD_USERS_DB_PATH;
import static com.car.sharing.app.util.TestConstants.CLEANUP_RENTALS_CONTROLLER_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.car.sharing.app.config.CustomMySQLContainer;
import com.car.sharing.app.dto.rental.CreateRentalRequest;
import com.car.sharing.app.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RentalControllerTest {
    private static final String RENTALS_PATH = "/rentals";

    static {
        CustomMySQLContainer container = CustomMySQLContainer.getInstance();
        container.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("CUSTOMER should create a rental")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CAR_WITH_STOCK_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void saveRental_AsCustomer_ReturnsCreated() throws Exception {
        CreateRentalRequest request = TestUtil.createRentalRequest(1L);

        mockMvc.perform(post(RENTALS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.car.id").value(1));
    }

    @Test
    @DisplayName("MANAGER should be forbidden from creating a rental")
    @WithUserDetails("manager@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CAR_WITH_STOCK_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void saveRental_AsManager_ReturnsForbidden() throws Exception {
        CreateRentalRequest request = TestUtil.createRentalRequest(1L);

        mockMvc.perform(post(RENTALS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return bad request when car is out of stock")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_OUT_OF_STOCK_CAR_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void saveRental_CarOutOfStock_ReturnsBadRequest() throws Exception {
        CreateRentalRequest request = TestUtil.createRentalRequest(1L);

        mockMvc.perform(post(RENTALS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return not found when creating rental for non-existent car")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void saveRental_NonExistentCar_ReturnsNotFound() throws Exception {
        CreateRentalRequest request = TestUtil.createRentalRequest(999L);

        mockMvc.perform(post(RENTALS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return bad request when return date is in the past")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CAR_WITH_STOCK_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void saveRental_PastReturnDate_ReturnsBadRequest() throws Exception {
        CreateRentalRequest request = new CreateRentalRequest(1L, java.time.LocalDate.now().minusDays(1));

        mockMvc.perform(post(RENTALS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CUSTOMER should get their own rentals")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CAR_WITH_STOCK_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getRentals_AsCustomer_ReturnsOwnRentals() throws Exception {
        mockMvc.perform(get(RENTALS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("MANAGER should get rentals by specifying userId")
    @WithUserDetails("manager@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CAR_WITH_STOCK_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getRentals_AsManager_ReturnsRentalsForSpecifiedUser() throws Exception {
        mockMvc.perform(get(RENTALS_PATH).param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Anonymous user should be unauthorized to list rentals")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getRentals_AsAnonymous_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(RENTALS_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CUSTOMER should get their own rental by id")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CAR_WITH_STOCK_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getRentalById_AsOwner_ReturnsRental() throws Exception {
        mockMvc.perform(get(RENTALS_PATH + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("CUSTOMER should not access another customer's rental")
    @WithUserDetails("other.customer@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CAR_WITH_STOCK_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getRentalById_AsOtherCustomer_ReturnsNotFound() throws Exception {
        mockMvc.perform(get(RENTALS_PATH + "/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("MANAGER should access any rental by id")
    @WithUserDetails("manager@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CAR_WITH_STOCK_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getRentalById_AsManager_ReturnsRental() throws Exception {
        mockMvc.perform(get(RENTALS_PATH + "/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CUSTOMER should set actual return date for their own rental")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CAR_WITH_STOCK_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void setActualReturnDate_AsOwner_ReturnsUpdatedRental() throws Exception {
        mockMvc.perform(post(RENTALS_PATH + "/1/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualReturnDate").isNotEmpty());
    }

    @Test
    @DisplayName("Should return bad request when rental is already returned")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_RENTALS_CONTROLLER_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CAR_WITH_STOCK_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_RENTAL_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void setActualReturnDate_AlreadyReturned_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post(RENTALS_PATH + "/1/return"));

        mockMvc.perform(post(RENTALS_PATH + "/1/return"))
                .andExpect(status().isBadRequest());
    }
}
