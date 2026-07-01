package com.car.sharing.app.controller.payment;

import com.car.sharing.app.dto.payment.CreatePaymentRequest;
import com.car.sharing.app.dto.payment.PaymentResponse;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.service.payment.PaymentService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments management", description = "Endpoints for managing payments")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "Get list of payments by user id",
            description = "Endpoint for getting list of payments by user id")
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER')")
    public Page<PaymentResponse> getPaymentsByUserId(
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal User user,
            @ParameterObject Pageable pageable
    ) {
        return paymentService.getPaymentsByUserId(userId, user, pageable);
    }

    @Operation(summary = "Create new payment", description = "Endpoint for creating new payment")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public PaymentResponse createPayment(@RequestBody @Valid CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping("/success")
    @Operation(summary = "Endpoint used by Stripe API to handle success request")
    public String handleSuccess(@RequestParam("session_id") String sessionId) {
        paymentService.processSuccess(sessionId);
        return "Payment successful! Return to website";
    }

    @GetMapping("/cancel")
    @Operation(summary = "Endpoint used by Stripe API to handle cancel request")
    public String handleCancel(@RequestParam("session_id") String sessionId) {
        paymentService.processCancel(sessionId);
        return "Payment canceled. You can return to website or try again";
    }
}
