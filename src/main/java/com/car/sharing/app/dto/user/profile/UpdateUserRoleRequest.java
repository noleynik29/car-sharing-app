package com.car.sharing.app.dto.user.profile;

import com.car.sharing.app.entity.User;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role cannot be null!")
        User.Role role
) {}
