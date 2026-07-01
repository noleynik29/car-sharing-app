package com.car.sharing.app.dto.rental;

import com.car.sharing.app.dto.car.CarResponse;
import java.time.LocalDate;

public record RentalResponse(
        Long id,
        LocalDate rentalDate,
        LocalDate returnDate,
        LocalDate actualReturnDate,
        CarResponse car,
        Long userId
) {}
