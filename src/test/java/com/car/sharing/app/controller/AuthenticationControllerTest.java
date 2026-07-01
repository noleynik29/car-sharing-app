package com.car.sharing.app.controller;

import static com.car.sharing.app.util.TestConstants.ADD_USER_DB_PATH;
import static com.car.sharing.app.util.TestConstants.CLEANUP_USERS_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.car.sharing.app.config.CustomMySQLContainer;
import com.car.sharing.app.dto.user.auth.UserLoginRequest;
import com.car.sharing.app.dto.user.auth.UserRegistrationRequest;
import com.car.sharing.app.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {
    private static final String AUTH_REGISTRATION_PATH = "/auth/registration";
    private static final String AUTH_LOGIN_PATH = "/auth/login";

    static {
        CustomMySQLContainer container = CustomMySQLContainer.getInstance();
        container.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return user response on registration")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void registration_ValidRequest_ReturnsResponse() throws Exception {
        UserRegistrationRequest request = TestUtil.createUserRegistrationRequest();

        mockMvc.perform(post(AUTH_REGISTRATION_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(request.email()))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Should return conflict when registering with existing email")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USER_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void registrationWithNonUniqueEmail_ReturnsConflict() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "john.doe@example.com", "John", "Doe", "password123", "password123");

        mockMvc.perform(post(AUTH_REGISTRATION_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return bad request when password and repeatPassword do not match")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void registrationWithMismatchedPasswords_ReturnsBadRequest() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "jane.doe@example.com", "Jane", "Doe", "password123", "differentPass1");

        mockMvc.perform(post(AUTH_REGISTRATION_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request when registration fields are blank")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void registrationWithBlankFields_ReturnsBadRequest() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "", "", "", "", "");

        mockMvc.perform(post(AUTH_REGISTRATION_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return token on valid login")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USER_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void login_ValidRequest_ReturnsToken() throws Exception {
        UserLoginRequest request = TestUtil.createUserLoginRequest();

        mockMvc.perform(post(AUTH_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Should return unauthorized on incorrect password")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USER_DB_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void loginWithIncorrectPassword_ReturnsUnauthorized() throws Exception {
        UserLoginRequest request = new UserLoginRequest("john.doe@example.com", "wrongpassword");

        mockMvc.perform(post(AUTH_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return unauthorized when email does not exist")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void loginWithNonExistentEmail_ReturnsUnauthorized() throws Exception {
        UserLoginRequest request = new UserLoginRequest("ghost@example.com", "password123");

        mockMvc.perform(post(AUTH_LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
