package com.car.sharing.app.controller;

import com.car.sharing.app.dto.rental.CreateRentalRequest;
import com.car.sharing.app.dto.rental.RentalResponse;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.service.rental.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rentals")
@RequiredArgsConstructor
@Tag(name = "Rentals management", description = "Endpoints for managing rentals")
public class RentalController {
    private final RentalService rentalService;

    @Operation(summary = "Create a rental by car id",
            description = "Endpoint for creating a rental")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    RentalResponse saveRental(@RequestBody @Valid CreateRentalRequest request,
                              @AuthenticationPrincipal User user) {
        return rentalService.save(user, request);
    }

    @Operation(summary = "Get list of rentals by user id",
            description = "Endpoint for getting list of rentals by user id "
                    + "whether the rental is still active or not")
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    public Page<RentalResponse> getRentals(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean isActive,
            @AuthenticationPrincipal User user,
            @ParameterObject Pageable pageable
    ) {
        return rentalService.getRentals(userId, isActive, user, pageable);
    }

    @Operation(summary = "Get rental by id",
            description = "Endpoint for getting specific rental by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    public RentalResponse getRentalById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return rentalService.getRentalById(user, id);
    }

    @Operation(summary = "Set actual return date for rental",
            description = "Endpoint for setting actual return date for rental")
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    public RentalResponse setActualReturnDate(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return rentalService.setActualReturnDate(user, id);
    }
}
