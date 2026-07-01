package com.car.sharing.app.controller;

import com.car.sharing.app.dto.car.CarResponse;
import com.car.sharing.app.dto.car.CreateCarRequest;
import com.car.sharing.app.dto.car.UpdateCarInventoryRequest;
import com.car.sharing.app.dto.car.UpdateCarRequest;
import com.car.sharing.app.service.car.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cars management", description = "Endpoints for managing cars")
@RestController
@RequiredArgsConstructor
@RequestMapping("/cars")
public class CarController {
    private final CarService carService;

    @Operation(summary = "Create a car", description = "Endpoint for creating a car")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER')")
    public CarResponse createCar(@RequestBody @Valid CreateCarRequest carRequest) {
        return carService.saveCar(carRequest);
    }

    @Operation(summary = "Get all cars", description = "Endpoint for getting all cars")
    @GetMapping
    public Page<CarResponse> getAllCars(@ParameterObject Pageable pageable) {
        return carService.getAll(pageable);
    }

    @Operation(summary = "Get car by id", description = "Endpoint for getting a car by id")
    @GetMapping("/{id}")
    public CarResponse getCarById(@PathVariable Long id) {
        return carService.getCarById(id);
    }

    @Operation(summary = "Update car", description = "Endpoint for updating car specs")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public CarResponse updateCar(@RequestBody @Valid UpdateCarRequest updateCarRequest,
                                 @PathVariable Long id) {
        return carService.updateCarById(updateCarRequest, id);
    }

    @Operation(summary = "Update car inventory",
            description = "Endpoint for updating car inventory")
    @PatchMapping("/{id}/inventory")
    @PreAuthorize("hasAnyRole('MANAGER')")
    public CarResponse updateCarInventory(
            @RequestBody @Valid UpdateCarInventoryRequest updateCarInventoryRequest,
            @PathVariable Long id
    ) {
        return carService.updateCarInventoryById(updateCarInventoryRequest, id);
    }

    @Operation(summary = "Delete car by id", description = "Endpoint for deleting car")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('MANAGER')")
    public void deleteCar(@PathVariable Long id) {
        carService.deleteCarById(id);
    }
}
