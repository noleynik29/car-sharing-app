package com.car.sharing.app.service.auth.impl;

import com.car.sharing.app.dto.user.UserResponse;
import com.car.sharing.app.dto.user.auth.UserLoginRequest;
import com.car.sharing.app.dto.user.auth.UserLoginResponse;
import com.car.sharing.app.dto.user.auth.UserRegistrationRequest;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.exception.RegistrationException;
import com.car.sharing.app.mapper.UserMapper;
import com.car.sharing.app.repository.user.UserRepository;
import com.car.sharing.app.security.JwtUtil;
import com.car.sharing.app.service.auth.AuthenticationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public UserResponse save(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RegistrationException("User with email "
                    + request.email()
                    + " already exists");
        }
        User user = userMapper.toModel(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(User.Role.CUSTOMER);

        return userMapper.toDto(userRepository.save(user));
    }

    public UserLoginResponse authenticate(UserLoginRequest request) {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String token = jwtUtil.generateToken(authentication.getName());
        return new UserLoginResponse(token);
    }
}
