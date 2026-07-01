package com.car.sharing.app.service.auth;

import com.car.sharing.app.dto.user.UserResponse;
import com.car.sharing.app.dto.user.auth.UserLoginRequest;
import com.car.sharing.app.dto.user.auth.UserLoginResponse;
import com.car.sharing.app.dto.user.auth.UserRegistrationRequest;

public interface AuthenticationService {
    UserResponse save(UserRegistrationRequest request);

    UserLoginResponse authenticate(UserLoginRequest request);
}
