package com.car.sharing.app.controller;

import static com.car.sharing.app.util.TestConstants.ADD_USERS_WITH_ADMIN_PATH;
import static com.car.sharing.app.util.TestConstants.CLEANUP_USERS_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.car.sharing.app.config.CustomMySQLContainer;
import com.car.sharing.app.dto.user.profile.UpdateUserRoleRequest;
import com.car.sharing.app.dto.user.profile.UserInfoUpdateRequest;
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
class UserControllerTest {
    private static final String USERS_PATH = "/users";

    static {
        CustomMySQLContainer container = CustomMySQLContainer.getInstance();
        container.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("ADMIN should update a user's role")
    @WithUserDetails("admin@example.com")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_WITH_ADMIN_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateUserRole_AsAdmin_ReturnsNoContent() throws Exception {
        UpdateUserRoleRequest request = TestUtil.updateUserRoleRequest();

        mockMvc.perform(put(USERS_PATH + "/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("MANAGER should be forbidden from updating a user's role")
    @WithUserDetails("manager@example.com")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_WITH_ADMIN_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateUserRole_AsManager_ReturnsForbidden() throws Exception {
        UpdateUserRoleRequest request = TestUtil.updateUserRoleRequest();

        mockMvc.perform(put(USERS_PATH + "/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CUSTOMER should be forbidden from updating a user's role")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_WITH_ADMIN_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateUserRole_AsCustomer_ReturnsForbidden() throws Exception {
        UpdateUserRoleRequest request = TestUtil.updateUserRoleRequest();

        mockMvc.perform(put(USERS_PATH + "/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return not found when updating role of non-existent user")
    @WithUserDetails("admin@example.com")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_WITH_ADMIN_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateUserRole_NonExistentUser_ReturnsNotFound() throws Exception {
        UpdateUserRoleRequest request = TestUtil.updateUserRoleRequest();

        mockMvc.perform(put(USERS_PATH + "/999/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return bad request when role is null")
    @WithUserDetails("admin@example.com")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_WITH_ADMIN_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateUserRole_NullRole_ReturnsBadRequest() throws Exception {
        String invalidJson = "{\"role\": null}";

        mockMvc.perform(put(USERS_PATH + "/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CUSTOMER should get their own profile info")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_WITH_ADMIN_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getCurrentUserInfo_AsCustomer_ReturnsOwnProfile() throws Exception {
        mockMvc.perform(get(USERS_PATH + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@example.com"));
    }

    @Test
    @DisplayName("MANAGER should be forbidden from getting profile via /me")
    @WithUserDetails("manager@example.com")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_WITH_ADMIN_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getCurrentUserInfo_AsManager_ReturnsForbidden() throws Exception {
        mockMvc.perform(get(USERS_PATH + "/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user should be unauthorized to get profile")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void getCurrentUserInfo_AsAnonymous_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(USERS_PATH + "/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CUSTOMER should update their own profile info")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_WITH_ADMIN_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateCurrentUserInfo_AsCustomer_ReturnsUpdatedProfile() throws Exception {
        UserInfoUpdateRequest request = TestUtil.userInfoUpdateRequest();

        mockMvc.perform(patch(USERS_PATH + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UpdatedFirst"))
                .andExpect(jsonPath("$.lastName").value("UpdatedLast"));
    }

    @Test
    @DisplayName("Should return bad request when first name exceeds max length")
    @WithUserDetails("customer@example.com")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_WITH_ADMIN_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateCurrentUserInfo_FirstNameTooLong_ReturnsBadRequest() throws Exception {
        UserInfoUpdateRequest request = new UserInfoUpdateRequest(
                "ThisFirstNameIsWayTooLongForValidation", "Doe");

        mockMvc.perform(patch(USERS_PATH + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("MANAGER should be forbidden from updating profile via /me")
    @WithUserDetails("manager@example.com")
    @Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = ADD_USERS_WITH_ADMIN_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void updateCurrentUserInfo_AsManager_ReturnsForbidden() throws Exception {
        UserInfoUpdateRequest request = TestUtil.userInfoUpdateRequest();

        mockMvc.perform(patch(USERS_PATH + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
