package com.car.sharing.app.service.payment;

import com.car.sharing.app.dto.payment.CreatePaymentRequest;
import com.car.sharing.app.dto.payment.PaymentResponse;
import com.car.sharing.app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    Page<PaymentResponse> getPaymentsByUserId(Long userId, User user, Pageable pageable);

    void processSuccess(String sessionId);

    void processCancel(String sessionId);
}
