package com.car.sharing.app.dto.notification;

import com.car.sharing.app.entity.Car;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;

public record Context(
        Rental rental,
        Car car,
        User user
) {}
