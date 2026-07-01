package com.car.sharing.app.dto.user.auth;

import com.car.sharing.app.validation.FieldMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@FieldMatch(field = "password",
        fieldToMatch = "repeatPassword")
public record UserRegistrationRequest(
        @NotBlank(message = "Email cannot be blank")
        @Size(min = 8, max = 50)
        String email,
        @NotBlank(message = "First name cannot be blank")
        @Size(min = 1, max = 20)
        String firstName,
        @NotBlank(message = "Last name cannot be blank")
        @Size(min = 1, max = 20)
        String lastName,
        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 20)
        String password,
        @NotBlank(message = "Repeat password cannot be blank")
        @Size(min = 8, max = 20)
        String repeatPassword
) {}
