package com.car.sharing.app.service.rental;

import com.car.sharing.app.dto.rental.CreateRentalRequest;
import com.car.sharing.app.dto.rental.RentalResponse;
import com.car.sharing.app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RentalService {
    RentalResponse save(User user, CreateRentalRequest createRentalRequest);

    Page<RentalResponse> getRentals(Long userId, Boolean isActive, User user, Pageable pageable);

    RentalResponse getRentalById(User user, Long id);

    RentalResponse setActualReturnDate(User user, Long id);
}
