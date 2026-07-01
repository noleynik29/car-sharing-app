package com.car.sharing.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.car.sharing.app.dto.car.CarResponse;
import com.car.sharing.app.dto.car.CreateCarRequest;
import com.car.sharing.app.dto.car.UpdateCarInventoryRequest;
import com.car.sharing.app.dto.car.UpdateCarRequest;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.exception.EntityNotFoundException;
import com.car.sharing.app.mapper.CarMapper;
import com.car.sharing.app.repository.car.CarRepository;
import com.car.sharing.app.service.car.impl.CarServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CarServiceImplTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    @InjectMocks
    private CarServiceImpl carService;

    private Car car;
    private CarResponse carResponse;

    @BeforeEach
    void setUp() {
        car = Car.builder()
                .id(1L)
                .model("Camry")
                .brand("Toyota")
                .type(Car.Type.SEDAN)
                .inventory(5)
                .dailyFee(BigDecimal.valueOf(45.50))
                .build();

        carResponse = new CarResponse(
                1L, "Camry", "Toyota", Car.Type.SEDAN, 5, BigDecimal.valueOf(45.50));
    }

    @Test
    @DisplayName("Should save car and return CarResponse")
    void saveCar_ValidRequest_ReturnsCarResponse() {
        CreateCarRequest request = new CreateCarRequest(
                "Camry", "Toyota", Car.Type.SEDAN, 5, BigDecimal.valueOf(45.50));

        when(carMapper.toModel(request)).thenReturn(car);
        when(carRepository.save(car)).thenReturn(car);
        when(carMapper.toDto(car)).thenReturn(carResponse);

        CarResponse actual = carService.saveCar(request);

        assertNotNull(actual);
        assertEquals(carResponse.model(), actual.model());
        assertEquals(carResponse.brand(), actual.brand());
        verify(carRepository).save(car);
    }

    @Test
    @DisplayName("Should map request to model before saving")
    void saveCar_ValidRequest_MapsRequestToModelFirst() {
        CreateCarRequest request = new CreateCarRequest(
                "Camry", "Toyota", Car.Type.SEDAN, 5, BigDecimal.valueOf(45.50));

        when(carMapper.toModel(request)).thenReturn(car);
        when(carRepository.save(car)).thenReturn(car);
        when(carMapper.toDto(car)).thenReturn(carResponse);

        carService.saveCar(request);

        verify(carMapper).toModel(request);
        verify(carRepository).save(car);
        verify(carMapper).toDto(car);
    }

    @Test
    @DisplayName("Should return page of CarResponse")
    void getAll_WithCars_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Car> carPage = new PageImpl<>(List.of(car));

        when(carRepository.findAll(pageable)).thenReturn(carPage);
        when(carMapper.toDto(car)).thenReturn(carResponse);

        Page<CarResponse> result = carService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(carResponse.model(), result.getContent().get(0).model());
        verify(carRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should return empty page when no cars exist")
    void getAll_NoCars_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(carRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<CarResponse> result = carService.getAll(pageable);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Should return CarResponse for existing id")
    void getCarById_ExistingId_ReturnsCarResponse() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carMapper.toDto(car)).thenReturn(carResponse);

        CarResponse actual = carService.getCarById(1L);

        assertNotNull(actual);
        assertEquals(carResponse.id(), actual.id());
        verify(carRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException for non-existent id")
    void getCarById_NonExistentId_ThrowsEntityNotFoundException() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> carService.getCarById(99L));
        verify(carMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should include id in EntityNotFoundException message")
    void getCarById_NonExistentId_ExceptionMessageContainsId() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> carService.getCarById(99L));

        assertEquals(true, ex.getMessage().contains("99"));
    }

    @Test
    @DisplayName("Should update car and return updated CarResponse")
    void updateCarById_ExistingId_ReturnsUpdatedCarResponse() {
        UpdateCarRequest request = new UpdateCarRequest(
                "Corolla", "Toyota", Car.Type.SEDAN, BigDecimal.valueOf(40.00));
        CarResponse updatedResponse = new CarResponse(
                1L, "Corolla", "Toyota", Car.Type.SEDAN, 5, BigDecimal.valueOf(40.00));

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carMapper.toDto(car)).thenReturn(updatedResponse);

        CarResponse actual = carService.updateCarById(request, 1L);

        assertNotNull(actual);
        assertEquals("Corolla", actual.model());
        verify(carMapper).updateCar(request, car);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent car")
    void updateCarById_NonExistentId_ThrowsEntityNotFoundException() {
        UpdateCarRequest request = new UpdateCarRequest(
                "Corolla", "Toyota", Car.Type.SEDAN, BigDecimal.valueOf(40.00));

        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> carService.updateCarById(request, 99L));
        verify(carMapper, never()).updateCar(any(), any());
    }

    @Test
    @DisplayName("Should update inventory and return updated CarResponse")
    void updateCarInventoryById_ExistingId_UpdatesInventory() {
        UpdateCarInventoryRequest request = new UpdateCarInventoryRequest(10);
        CarResponse updatedResponse = new CarResponse(
                1L, "Camry", "Toyota", Car.Type.SEDAN, 10, BigDecimal.valueOf(45.50));

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carMapper.toDto(car)).thenReturn(updatedResponse);

        CarResponse actual = carService.updateCarInventoryById(request, 1L);

        assertEquals(10, actual.inventory());
        assertEquals(10, car.getInventory());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating inventory of non-existent car")
    void updateCarInventoryById_NonExistentId_ThrowsEntityNotFoundException() {
        UpdateCarInventoryRequest request = new UpdateCarInventoryRequest(10);

        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> carService.updateCarInventoryById(request, 99L));
    }

    @Test
    @DisplayName("Should set inventory directly on entity")
    void updateCarInventoryById_ExistingId_SetsInventoryOnEntity() {
        UpdateCarInventoryRequest request = new UpdateCarInventoryRequest(10);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carMapper.toDto(car)).thenReturn(carResponse);

        carService.updateCarInventoryById(request, 1L);

        assertEquals(10, car.getInventory());
    }

    @Test
    @DisplayName("Should delete car when it exists")
    void deleteCarById_ExistingId_DeletesCar() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        carService.deleteCarById(1L);

        verify(carRepository).delete(car);
    }

    @Test
    @DisplayName("Should do nothing when car does not exist")
    void deleteCarById_NonExistentId_DoesNothing() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        carService.deleteCarById(99L);

        verify(carRepository, never()).delete(any());
    }
}