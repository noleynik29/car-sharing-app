package com.car.sharing.app.service.car;

import com.car.sharing.app.dto.car.CarResponse;
import com.car.sharing.app.dto.car.CreateCarRequest;
import com.car.sharing.app.dto.car.UpdateCarInventoryRequest;
import com.car.sharing.app.dto.car.UpdateCarRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CarService {
    CarResponse saveCar(CreateCarRequest carRequest);

    Page<CarResponse> getAll(Pageable pageable);

    CarResponse getCarById(Long id);

    CarResponse updateCarById(UpdateCarRequest updateCarRequest, Long id);

    CarResponse updateCarInventoryById(
            UpdateCarInventoryRequest updateCarInventoryRequest,
            Long id
    );

    void deleteCarById(Long id);
}
