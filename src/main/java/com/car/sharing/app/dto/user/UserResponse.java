package com.car.sharing.app.dto.user;

import com.car.sharing.app.entity.User;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        User.Role role
) {}
