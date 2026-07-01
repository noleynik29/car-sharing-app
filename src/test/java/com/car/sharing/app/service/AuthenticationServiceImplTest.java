package com.car.sharing.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.car.sharing.app.dto.user.UserResponse;
import com.car.sharing.app.dto.user.auth.UserLoginRequest;
import com.car.sharing.app.dto.user.auth.UserLoginResponse;
import com.car.sharing.app.dto.user.auth.UserRegistrationRequest;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.exception.RegistrationException;
import com.car.sharing.app.mapper.UserMapper;
import com.car.sharing.app.repository.user.UserRepository;
import com.car.sharing.app.security.JwtUtil;
import com.car.sharing.app.service.auth.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    @DisplayName("Should register a new user and return UserResponse")
    void save_ValidRequest_ReturnsUserResponse() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "john.doe@example.com", "John", "Doe", "password123", "password123");
        User user = new User();
        user.setEmail("john.doe@example.com");
        UserResponse expected = new UserResponse(
                1L, "john.doe@example.com", "John", "Doe", User.Role.CUSTOMER);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toModel(request)).thenReturn(user);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expected);

        UserResponse actual = authenticationService.save(request);

        assertNotNull(actual);
        assertEquals(expected.email(), actual.email());
        assertEquals(User.Role.CUSTOMER, actual.role());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should encode password before saving user")
    void save_ValidRequest_EncodesPassword() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "john.doe@example.com", "John", "Doe", "password123", "password123");
        User user = new User();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toModel(request)).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(
                new UserResponse(1L, "john.doe@example.com", "John", "Doe", User.Role.CUSTOMER));

        authenticationService.save(request);

        verify(passwordEncoder).encode("password123");
        assertEquals("encoded-password", user.getPassword());
    }

    @Test
    @DisplayName("Should set role to CUSTOMER on registration")
    void save_ValidRequest_SetsCustomerRole() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "john.doe@example.com", "John", "Doe", "password123", "password123");
        User user = new User();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toModel(request)).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(
                new UserResponse(1L, "john.doe@example.com", "John", "Doe", User.Role.CUSTOMER));

        authenticationService.save(request);

        assertEquals(User.Role.CUSTOMER, user.getRole());
    }

    @Test
    @DisplayName("Should throw RegistrationException when email already exists")
    void save_DuplicateEmail_ThrowsRegistrationException() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "existing@example.com", "John", "Doe", "password123", "password123");

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(RegistrationException.class, () -> authenticationService.save(request));
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toModel(any());
    }

    @Test
    @DisplayName("Should include email in RegistrationException message")
    void save_DuplicateEmail_ExceptionMessageContainsEmail() {
        String email = "existing@example.com";
        UserRegistrationRequest request = new UserRegistrationRequest(
                email, "John", "Doe", "password123", "password123");

        when(userRepository.existsByEmail(email)).thenReturn(true);

        RegistrationException ex = assertThrows(RegistrationException.class,
                () -> authenticationService.save(request));

        assertEquals(true, ex.getMessage().contains(email));
    }

    @Test
    @DisplayName("Should return token on successful authentication")
    void authenticate_ValidCredentials_ReturnsToken() {
        UserLoginRequest request = new UserLoginRequest("john.doe@example.com", "password123");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "john.doe@example.com", null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken("john.doe@example.com")).thenReturn("mocked-jwt-token");

        UserLoginResponse response = authenticationService.authenticate(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.token());
        verify(jwtUtil).generateToken("john.doe@example.com");
    }

    @Test
    @DisplayName("Should use email from authenticated principal to generate token")
    void authenticate_ValidCredentials_UsesEmailFromAuthentication() {
        UserLoginRequest request = new UserLoginRequest("john.doe@example.com", "password123");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "john.doe@example.com", null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(anyString())).thenReturn("mocked-jwt-token");

        authenticationService.authenticate(request);

        verify(jwtUtil).generateToken("john.doe@example.com");
    }

    @Test
    @DisplayName("Should throw when AuthenticationManager rejects credentials")
    void authenticate_InvalidCredentials_ThrowsBadCredentialsException() {
        UserLoginRequest request = new UserLoginRequest("john.doe@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authenticationService.authenticate(request));
        verify(jwtUtil, never()).generateToken(anyString());
    }
}
