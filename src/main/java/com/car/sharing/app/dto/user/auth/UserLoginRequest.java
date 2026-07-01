package com.car.sharing.app.dto.user.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
        @NotBlank(message = "Email cannot be blank")
        @Size(min = 8, max = 50)
        String email,
        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 20)
        String password
) {}
