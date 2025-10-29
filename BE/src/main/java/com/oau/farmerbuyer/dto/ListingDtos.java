package com.oau.farmerbuyer.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public class ListingDtos {

    public record Create(
            @NotNull @Positive Long farmerId,
            @NotNull @Positive Long cropId,
            @NotBlank @Size(max = 160) String title,
            String description,
            @NotBlank String unit, // Will be validated as valid Crop.Unit
            @NotNull @DecimalMin("0.001") BigDecimal quantityAvailable,
            @DecimalMin("0.001") BigDecimal minOrderQty, // Optional, defaults to 1
            @NotNull @DecimalMin("0.01") BigDecimal pricePerUnitNgn,
            String locationText,
            @DecimalMin("-90") @DecimalMax("90") Double lat,
            @DecimalMin("-180") @DecimalMax("180") Double lng
    ) {}

    public record Update(
            @Size(max = 160) String title,
            String description,
            String unit,
            @DecimalMin("0.001") BigDecimal quantityAvailable,
            @DecimalMin("0.001") BigDecimal minOrderQty,
            @DecimalMin("0.01") BigDecimal pricePerUnitNgn,
            String locationText,
            @DecimalMin("-90") @DecimalMax("90") Double lat,
            @DecimalMin("-180") @DecimalMax("180") Double lng,
            String status // DRAFT, ACTIVE, SOLD_OUT, ARCHIVED
    ) {}

    public record Response(
            Long id,
            String title,
            String description,
            String cropName,
            String unit,
            BigDecimal quantityAvailable,
            BigDecimal minOrderQty,
            BigDecimal pricePerUnitNgn,
            String locationText,
            Double lat,
            Double lng,
            String status,
            Integer photosCount,
            String farmerName, // Added for better UX
            Instant createdAt,
            Instant updatedAt,
            Boolean isFeatured
    ) {}

    public record Summary(
            Long id,
            String title,
            String cropName,
            String unit,
            BigDecimal pricePerUnitNgn,
            BigDecimal quantityAvailable,
            String locationText,
            String status,
            Boolean isFeatured
    ) {}
}
