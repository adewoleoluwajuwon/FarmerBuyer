// src/main/java/com/oau/farmerbuyer/dto/OrderDtos.java
package com.oau.farmerbuyer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class OrderDtos {

    public enum DeliveryOption { PICKUP, DELIVERY }

    /**
     * Create request:
     * - No buyerId here (taken from Authentication)
     * - listingId is required
     * - quantityOrdered >= 1
     * - deliveryOption required
     * - optional addressId/notes
     * - idemKey for idempotency
     */
    public record Create(
            @NotNull Long listingId,
            @NotNull @Min(1) BigDecimal quantityOrdered,
            @NotNull DeliveryOption deliveryOption,
            Long addressId,
            String notes,
            @NotBlank String idemKey
    ) {}

    /**
     * Minimal response (compatible with your FE expectations).
     * Extend as needed (e.g., add fields like paymentReference, snapshots, etc.).
     */
    public record Response(
            Long id,
            String status,
            BigDecimal totalNgn
    ) {}
}
