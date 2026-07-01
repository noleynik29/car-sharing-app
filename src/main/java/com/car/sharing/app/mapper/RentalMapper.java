package com.car.sharing.app.mapper;

import com.car.sharing.app.config.MapperConfig;
import com.car.sharing.app.dto.rental.CreateRentalRequest;
import com.car.sharing.app.dto.rental.RentalResponse;
import com.car.sharing.app.entity.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface RentalMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "car", target = "car")
    RentalResponse toDto(Rental rental);

    Rental toModel(CreateRentalRequest createRentalRequest);
}
