// src/main/java/com/oau/farmerbuyer/web/OrderController.java
package com.oau.farmerbuyer.web;

import com.oau.farmerbuyer.dto.OrderDtos;
import com.oau.farmerbuyer.security.SecurityUtils;
import com.oau.farmerbuyer.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    /**
     * Create a PENDING order (idempotent via header).
     * - Reads buyerId from the authenticated principal
     * - Body does NOT contain buyerId anymore
     * - Service must create order in PENDING and snapshot price
     */
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public OrderDtos.Response createPending(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @Valid @RequestBody OrderDtos.Create req,
            Authentication auth
    ) {
        Long buyerId = SecurityUtils.currentUserId(auth);
        return service.createPendingOrder(buyerId, req, idemKey);
    }

    // ⭐ List current buyer's orders
    @PreAuthorize("hasAnyRole('BUYER','ADMIN')")
    @GetMapping
    public Page<OrderDtos.Response> myOrders(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth
    ) {
        Long buyerId = SecurityUtils.currentUserId(auth);
        return service.myOrders(buyerId, pageable);
    }

    // ⭐ Get one order if owned by current buyer (or admin)
    @PreAuthorize("hasAnyRole('BUYER','ADMIN')")
    @GetMapping("/{id}")
    public OrderDtos.Response getOne(@PathVariable Long id, Authentication auth) {
        Long buyerId = SecurityUtils.currentUserId(auth);
        return service.getOne(id, buyerId);
    }
}
