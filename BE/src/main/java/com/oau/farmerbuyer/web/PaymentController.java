package com.oau.farmerbuyer.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// src/main/java/com/oau/farmerbuyer/web/PaymentController.java
@RestController
@RequestMapping("/api/payments") @RequiredArgsConstructor
public class PaymentController {
    @PostMapping("/init")
    public Map<String,String> init(@RequestBody Map<String,Object> body) {
        // TODO: generate real provider session
        String redirect = "https://paystack.com/pay/demo"; // placeholder
        return Map.of("redirectUrl", redirect);
    }
}
