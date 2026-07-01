package com.car.sharing.app.dto.payment;

import com.car.sharing.app.entity.Payment;
import java.math.BigDecimal;
import java.net.URL;

public record PaymentResponse(
        Long id,
        Payment.Status status,
        Payment.Type type,
        Long rentalId,
        URL sessionUrl,
        String sessionId,
        BigDecimal amountToPay
) {}
