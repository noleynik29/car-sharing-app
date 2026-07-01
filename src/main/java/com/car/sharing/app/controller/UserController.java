package com.car.sharing.app.controller;

import com.car.sharing.app.dto.user.UserResponse;
import com.car.sharing.app.dto.user.profile.UpdateUserRoleRequest;
import com.car.sharing.app.dto.user.profile.UserInfoUpdateRequest;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User management", description = "Endpoints for managing user auth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @Operation(summary = "Update user role", description = "Update user's role by id")
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> updateUserRole(@PathVariable Long id,
                                        @RequestBody @Valid UpdateUserRoleRequest request
    ) {
        userService.updateUserRole(id, request);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get my profile info",
            description = "Get current user's profile info")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    UserResponse getCurrentUserInfo(@AuthenticationPrincipal User user) {
        return userService.getUserInfo(user);
    }

    @Operation(summary = "Update my profile info",
            description = "Update current user's profile info")
    @PatchMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    UserResponse updateCurrentUserInfo(@AuthenticationPrincipal User user,
                                       @RequestBody @Valid UserInfoUpdateRequest request
    ) {
        return userService.updateUserInfo(user, request);
    }
}
