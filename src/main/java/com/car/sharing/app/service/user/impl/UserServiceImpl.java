package com.car.sharing.app.service.user.impl;

import com.car.sharing.app.dto.user.UserResponse;
import com.car.sharing.app.dto.user.profile.UpdateUserRoleRequest;
import com.car.sharing.app.dto.user.profile.UserInfoUpdateRequest;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.exception.EntityNotFoundException;
import com.car.sharing.app.mapper.UserMapper;
import com.car.sharing.app.repository.user.UserRepository;
import com.car.sharing.app.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public void updateUserRole(Long id, UpdateUserRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User by id: " + id + " not found"));

        user.setRole(request.role());
        userRepository.save(user);
    }

    @Override
    public UserResponse getUserInfo(User user) {
        return userMapper.toDto(user);
    }

    @Override
    public UserResponse updateUserInfo(User user, UserInfoUpdateRequest request) {
        userMapper.updateUser(request, user);

        return userMapper.toDto(userRepository.save(user));
    }
}
