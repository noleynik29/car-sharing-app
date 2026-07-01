package com.car.sharing.app.dto.payment;

import com.car.sharing.app.entity.Payment;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull(message = "Rental ID cannot be null!")
        Long rentalId,

        @NotNull(message = "Payment type cannot be null!")
        Payment.Type paymentType
) {}
