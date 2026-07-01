package com.car.sharing.app.dto.user.profile;

import jakarta.validation.constraints.Size;

public record UserInfoUpdateRequest(
        @Size(min = 1, max = 20, message = "First name must be between 1 and 20 characters long!")
        String firstName,
        @Size(min = 1, max = 20, message = "Last name must be between 1 and 20 characters long!")
        String lastName
) {}
