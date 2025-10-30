// src/main/java/com/oau/farmerbuyer/web/PaymentController.java
package com.oau.farmerbuyer.web;

import com.oau.farmerbuyer.payments.dto.PaymentInitRequest;
import com.oau.farmerbuyer.payments.dto.PaymentInitResponse;
import com.oau.farmerbuyer.security.SecurityUtils;
import com.oau.farmerbuyer.service.PaymentService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/init")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<PaymentInitResponse> init(@RequestBody PaymentInitRequest req,
                                                    Authentication auth) {
        Long buyerId = SecurityUtils.currentUserId(auth);
        PaymentInitResponse resp = paymentService.initPaystackPayment(buyerId, req.getOrderId());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/paystack/webhook")
    @PermitAll
    public ResponseEntity<Void> paystackWebhook(@RequestHeader("x-paystack-signature") String sig,
                                                @RequestBody String rawBody) {
        paymentService.handlePaystackWebhook(sig, rawBody);
        return ResponseEntity.ok().build();
    }
}
