package com.car.sharing.app.mapper;

import com.car.sharing.app.config.MapperConfig;
import com.car.sharing.app.dto.car.CarResponse;
import com.car.sharing.app.dto.car.CreateCarRequest;
import com.car.sharing.app.dto.car.UpdateCarRequest;
import com.car.sharing.app.entity.Car;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapperConfig.class)
public interface CarMapper {
    CarResponse toDto(Car car);

    @Mapping(source = "carType", target = "carType")
    Car toModel(CreateCarRequest carRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCar(UpdateCarRequest carRequest, @MappingTarget Car car);
}
