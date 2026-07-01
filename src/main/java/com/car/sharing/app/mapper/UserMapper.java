package com.car.sharing.app.mapper;

import com.car.sharing.app.config.MapperConfig;
import com.car.sharing.app.dto.user.UserResponse;
import com.car.sharing.app.dto.user.auth.UserRegistrationRequest;
import com.car.sharing.app.dto.user.profile.UserInfoUpdateRequest;
import com.car.sharing.app.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    UserResponse toDto(User user);

    User toModel(UserRegistrationRequest userRegistrationRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUser(UserInfoUpdateRequest request, @MappingTarget User user);
}
