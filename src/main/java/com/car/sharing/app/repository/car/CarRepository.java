package com.car.sharing.app.repository.car;

import com.car.sharing.app.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarRepository extends JpaRepository<Car, Long> {
}

