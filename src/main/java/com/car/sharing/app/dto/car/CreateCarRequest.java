package com.car.sharing.app.dto.car;

import com.car.sharing.app.entity.Car;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCarRequest(
        @NotBlank(message = "Model name cannot be blank!")
        @Size(max = 30, message = "Model name cannot be more than 30 characters long!")
        String model,
        @NotBlank(message = "Brand name cannot be blank!")
        @Size(max = 30, message = "Brand name cannot be more than 30 characters long!")
        String brand,
        @NotNull(message = "Car type cannot be null!")
        Car.Type carType,
        @PositiveOrZero(message = "Inventory must be positive or zero!")
        int inventory,
        @NotNull(message = "Daily fee cannot be null!")
        @DecimalMin(value = "0.01", message = "Daily fee must be greater than 0!")
        BigDecimal dailyFee
) {}
