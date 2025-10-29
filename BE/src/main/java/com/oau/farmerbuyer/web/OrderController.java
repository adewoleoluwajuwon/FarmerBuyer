package com.oau.farmerbuyer.web;

import com.oau.farmerbuyer.dto.OrderDtos;
import com.oau.farmerbuyer.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public OrderDtos.Response place(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @Valid @RequestBody OrderDtos.Create dto) {
        return service.place(dto, idemKey);
    }
}

