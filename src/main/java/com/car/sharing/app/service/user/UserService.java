package com.car.sharing.app.service.user;

import com.car.sharing.app.dto.user.UserResponse;
import com.car.sharing.app.dto.user.profile.UpdateUserRoleRequest;
import com.car.sharing.app.dto.user.profile.UserInfoUpdateRequest;
import com.car.sharing.app.entity.User;

public interface UserService {
    void updateUserRole(Long id, UpdateUserRoleRequest request);

    UserResponse getUserInfo(User user);

    UserResponse updateUserInfo(User user, UserInfoUpdateRequest request);
}
