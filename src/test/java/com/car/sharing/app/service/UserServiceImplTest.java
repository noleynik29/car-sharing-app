package com.car.sharing.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.car.sharing.app.dto.user.UserResponse;
import com.car.sharing.app.dto.user.profile.UpdateUserRoleRequest;
import com.car.sharing.app.dto.user.profile.UserInfoUpdateRequest;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.exception.EntityNotFoundException;
import com.car.sharing.app.mapper.UserMapper;
import com.car.sharing.app.repository.user.UserRepository;
import com.car.sharing.app.service.user.impl.UserServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("customer@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("encoded-password")
                .role(User.Role.CUSTOMER)
                .isDeleted(false)
                .build();

        userResponse = new UserResponse(
                1L, "customer@example.com", "John", "Doe", User.Role.CUSTOMER);
    }

    @Test
    @DisplayName("Should update user role and save")
    void updateUserRole_ExistingUser_UpdatesRoleAndSaves() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(User.Role.MANAGER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateUserRole(1L, request);

        assertEquals(User.Role.MANAGER, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found by id")
    void updateUserRole_UserNotFound_ThrowsEntityNotFoundException() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(User.Role.MANAGER);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.updateUserRole(99L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should include id in EntityNotFoundException message")
    void updateUserRole_UserNotFound_ExceptionMessageContainsId() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(User.Role.MANAGER);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> userService.updateUserRole(99L, request));

        assertEquals(true, ex.getMessage().contains("99"));
    }

    @Test
    @DisplayName("Should not change role when same role is passed")
    void updateUserRole_SameRole_SavesWithSameRole() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(User.Role.CUSTOMER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateUserRole(1L, request);

        assertEquals(User.Role.CUSTOMER, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should return UserResponse for the authenticated user")
    void getUserInfo_AuthenticatedUser_ReturnsUserResponse() {
        when(userMapper.toDto(user)).thenReturn(userResponse);

        UserResponse actual = userService.getUserInfo(user);

        assertNotNull(actual);
        assertEquals(userResponse.email(), actual.email());
        assertEquals(userResponse.role(), actual.role());
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Should not call repository for getUserInfo")
    void getUserInfo_AuthenticatedUser_DoesNotCallRepository() {
        when(userMapper.toDto(user)).thenReturn(userResponse);

        userService.getUserInfo(user);

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update user info and return updated UserResponse")
    void updateUserInfo_ValidRequest_ReturnsUpdatedUserResponse() {
        UserInfoUpdateRequest request = new UserInfoUpdateRequest("UpdatedFirst", "UpdatedLast");
        UserResponse updatedResponse = new UserResponse(
                1L, "customer@example.com", "UpdatedFirst", "UpdatedLast", User.Role.CUSTOMER);

        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(updatedResponse);

        UserResponse actual = userService.updateUserInfo(user, request);

        assertNotNull(actual);
        assertEquals("UpdatedFirst", actual.firstName());
        assertEquals("UpdatedLast", actual.lastName());
        verify(userMapper).updateUser(request, user);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should call mapper updateUser before saving")
    void updateUserInfo_ValidRequest_CallsMapperBeforeSave() {
        UserInfoUpdateRequest request = new UserInfoUpdateRequest("UpdatedFirst", "UpdatedLast");

        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userResponse);

        userService.updateUserInfo(user, request);

        var inOrder = org.mockito.Mockito.inOrder(userMapper, userRepository);
        inOrder.verify(userMapper).updateUser(request, user);
        inOrder.verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should update only firstName when lastName is null")
    void updateUserInfo_NullLastName_UpdatesOnlyFirstName() {
        UserInfoUpdateRequest request = new UserInfoUpdateRequest("UpdatedFirst", null);
        UserResponse updatedResponse = new UserResponse(
                1L, "customer@example.com", "UpdatedFirst", "Doe", User.Role.CUSTOMER);

        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(updatedResponse);

        UserResponse actual = userService.updateUserInfo(user, request);

        assertEquals("UpdatedFirst", actual.firstName());
        assertEquals("Doe", actual.lastName());
        verify(userMapper).updateUser(request, user);
    }
}
