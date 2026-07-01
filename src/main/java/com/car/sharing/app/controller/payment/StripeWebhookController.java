package com.car.sharing.app.controller.payment;

import com.car.sharing.app.service.payment.impl.StripeWebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stripe webhooks",
        description = "Webhooks used by Stripe to manipulate payments")
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks")
public class StripeWebhookController {
    private final StripeWebhookService stripeWebhookService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            stripeWebhookService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error");
        }
    }
}
