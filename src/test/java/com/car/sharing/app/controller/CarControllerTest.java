package com.car.sharing.app.controller;

import static com.car.sharing.app.util.TestConstants.ADD_CARS_DB_PATH;
import static com.car.sharing.app.util.TestConstants.CLEANUP_CARS_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.car.sharing.app.config.CustomMySQLContainer;
import com.car.sharing.app.dto.car.CreateCarRequest;
import com.car.sharing.app.dto.car.UpdateCarInventoryRequest;
import com.car.sharing.app.dto.car.UpdateCarRequest;
import com.car.sharing.app.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CarControllerTest {
    private static final String CARS_PATH = "/cars";

    static {
        CustomMySQLContainer container = CustomMySQLContainer.getInstance();
        container.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("MANAGER should create a car")
    @WithMockUser(roles = "MANAGER")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createCar_AsManager_ReturnsCreated() throws Exception {
        CreateCarRequest request = TestUtil.createCarRequest();

        mockMvc.perform(post(CARS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.model").value(request.model()))
                .andExpect(jsonPath("$.brand").value(request.brand()));
    }

    @Test
    @DisplayName("CUSTOMER should be forbidden from creating a car")
    @WithMockUser(roles = "CUSTOMER")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createCar_AsCustomer_ReturnsForbidden() throws Exception {
        CreateCarRequest request = TestUtil.createCarRequest();

        mockMvc.perform(post(CARS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user should be unauthorized to create a car")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createCar_AsAnonymous_ReturnsUnauthorized() throws Exception {
        CreateCarRequest request = TestUtil.createCarRequest();

        mockMvc.perform(post(CARS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return bad request when creating a car with invalid data")
    @WithMockUser(roles = "MANAGER")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void createCar_InvalidRequest_ReturnsBadRequest() throws Exception {
        CreateCarRequest request = new CreateCarRequest("", "", null, -1, BigDecimal.ZERO);

        mockMvc.perform(post(CARS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get paginated list of all cars - public access")
    @WithMockUser
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CARS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getAllCars_ReturnsPagedResults() throws Exception {
        mockMvc.perform(get(CARS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    @DisplayName("Should get car by id - public access")
    @WithMockUser
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CARS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getCarById_ExistingId_ReturnsCar() throws Exception {
        mockMvc.perform(get(CARS_PATH + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Toyota"));
    }

    @Test
    @DisplayName("Should return not found when car id does not exist")
    @WithMockUser
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getCarById_NonExistentId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get(CARS_PATH + "/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("MANAGER should update a car")
    @WithMockUser(roles = "MANAGER")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CARS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateCar_AsManager_ReturnsUpdatedCar() throws Exception {
        UpdateCarRequest request = TestUtil.updateCarRequest();

        mockMvc.perform(patch(CARS_PATH + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value(request.model()))
                .andExpect(jsonPath("$.dailyFee").value(42.00));
    }

    @Test
    @DisplayName("CUSTOMER should be forbidden from updating a car")
    @WithMockUser(roles = "CUSTOMER")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CARS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateCar_AsCustomer_ReturnsForbidden() throws Exception {
        UpdateCarRequest request = TestUtil.updateCarRequest();

        mockMvc.perform(patch(CARS_PATH + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return not found when updating a non-existent car")
    @WithMockUser(roles = "MANAGER")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateCar_NonExistentId_ReturnsNotFound() throws Exception {
        UpdateCarRequest request = TestUtil.updateCarRequest();

        mockMvc.perform(patch(CARS_PATH + "/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("MANAGER should update car inventory")
    @WithMockUser(roles = "MANAGER")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CARS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateCarInventory_AsManager_ReturnsUpdatedCar() throws Exception {
        UpdateCarInventoryRequest request = TestUtil.updateCarInventoryRequest();

        mockMvc.perform(patch(CARS_PATH + "/1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventory").value(10));
    }

    @Test
    @DisplayName("Should return bad request when inventory is negative")
    @WithMockUser(roles = "MANAGER")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CARS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateCarInventory_NegativeValue_ReturnsBadRequest() throws Exception {
        UpdateCarInventoryRequest request = new UpdateCarInventoryRequest(-5);

        mockMvc.perform(patch(CARS_PATH + "/1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("MANAGER should delete a car")
    @WithMockUser(roles = "MANAGER")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CARS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void deleteCar_AsManager_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete(CARS_PATH + "/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CARS_PATH + "/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CUSTOMER should be forbidden from deleting a car")
    @WithMockUser(roles = "CUSTOMER")
    @Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_CARS_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void deleteCar_AsCustomer_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete(CARS_PATH + "/1"))
                .andExpect(status().isForbidden());
    }
}
