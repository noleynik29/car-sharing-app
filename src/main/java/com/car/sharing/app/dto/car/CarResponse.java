package com.car.sharing.app.dto.car;

import com.car.sharing.app.entity.Car;
import java.math.BigDecimal;

public record CarResponse(
        Long id,
        String model,
        String brand,
        Car.Type carType,
        int inventory,
        BigDecimal dailyFee
) {}
