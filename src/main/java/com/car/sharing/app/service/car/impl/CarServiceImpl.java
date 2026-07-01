package com.car.sharing.app.service.car.impl;

import com.car.sharing.app.dto.car.CarResponse;
import com.car.sharing.app.dto.car.CreateCarRequest;
import com.car.sharing.app.dto.car.UpdateCarInventoryRequest;
import com.car.sharing.app.dto.car.UpdateCarRequest;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.exception.EntityNotFoundException;
import com.car.sharing.app.mapper.CarMapper;
import com.car.sharing.app.repository.car.CarRepository;
import com.car.sharing.app.service.car.CarService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CarMapper carMapper;

    @Override
    public CarResponse saveCar(CreateCarRequest carRequest) {
        return carMapper.toDto(carRepository.save(carMapper.toModel(carRequest)));
    }

    @Override
    public Page<CarResponse> getAll(Pageable pageable) {
        return carRepository.findAll(pageable)
                .map(carMapper::toDto);
    }

    @Override
    public CarResponse getCarById(Long id) {
        Car car = findCarOrThrow(id);

        return carMapper.toDto(car);
    }

    @Transactional
    @Override
    public CarResponse updateCarById(UpdateCarRequest request, Long id) {
        Car car = findCarOrThrow(id);

        carMapper.updateCar(request, car);

        return carMapper.toDto(car);
    }

    @Transactional
    @Override
    public CarResponse updateCarInventoryById(UpdateCarInventoryRequest request, Long id) {
        Car car = findCarOrThrow(id);

        car.setInventory(request.inventory());

        return carMapper.toDto(car);
    }

    @Override
    public void deleteCarById(Long id) {
        carRepository.findById(id).ifPresent(carRepository::delete);
    }

    private Car findCarOrThrow(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car by id: " + id + " not found!")
                );
    }
}
