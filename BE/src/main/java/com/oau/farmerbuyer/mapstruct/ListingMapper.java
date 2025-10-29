package com.oau.farmerbuyer.mapstruct;

import com.oau.farmerbuyer.domain.Listing;
import com.oau.farmerbuyer.domain.Crop;
import com.oau.farmerbuyer.dto.ListingDtos;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class ListingMapper {

    public Listing toEntity(ListingDtos.Create dto) {
        return Listing.builder()
                .title(dto.title())
                .description(dto.description())
                .unit(Crop.Unit.valueOf(dto.unit().toUpperCase()))
                .quantityAvailable(dto.quantityAvailable())
                .minOrderQty(dto.minOrderQty() != null ? dto.minOrderQty() : BigDecimal.ONE)
                .pricePerUnitNgn(dto.pricePerUnitNgn())
                .locationText(dto.locationText())
                .lat(dto.lat() != null ? BigDecimal.valueOf(dto.lat()) : null)
                .lng(dto.lng() != null ? BigDecimal.valueOf(dto.lng()) : null)
                .status(Listing.Status.ACTIVE)
                .photosCount(0)
                .build();
    }

    public void updateEntity(Listing entity, ListingDtos.Update dto) {
        if (dto.title() != null) entity.setTitle(dto.title());
        if (dto.description() != null) entity.setDescription(dto.description());
        if (dto.unit() != null) entity.setUnit(Crop.Unit.valueOf(dto.unit().toUpperCase()));
        if (dto.quantityAvailable() != null) entity.setQuantityAvailable(dto.quantityAvailable());
        if (dto.minOrderQty() != null) entity.setMinOrderQty(dto.minOrderQty());
        if (dto.pricePerUnitNgn() != null) entity.setPricePerUnitNgn(dto.pricePerUnitNgn());
        if (dto.locationText() != null) entity.setLocationText(dto.locationText());
        if (dto.lat() != null) entity.setLat(BigDecimal.valueOf(dto.lat()));
        if (dto.lng() != null) entity.setLng(BigDecimal.valueOf(dto.lng()));
        if (dto.status() != null) entity.setStatus(Listing.Status.valueOf(dto.status().toUpperCase()));
    }

    public ListingDtos.Response toResponse(Listing entity) {
        return new ListingDtos.Response(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCrop().getName(),
                entity.getUnit().name(),
                entity.getQuantityAvailable(),
                entity.getMinOrderQty(),
                entity.getPricePerUnitNgn(),
                entity.getLocationText(),
                entity.getLat() != null ? entity.getLat().doubleValue() : null,
                entity.getLng() != null ? entity.getLng().doubleValue() : null,
                entity.getStatus().name(),
                entity.getPhotosCount(),
                entity.getFarmer().getFullName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getFeaturedUntil() != null && entity.getFeaturedUntil().isAfter(Instant.now())
        );
    }

    public ListingDtos.Summary toSummary(Listing entity) {
        return new ListingDtos.Summary(
                entity.getId(),
                entity.getTitle(),
                entity.getCrop().getName(),
                entity.getUnit().name(),
                entity.getPricePerUnitNgn(),
                entity.getQuantityAvailable(),
                entity.getLocationText(),
                entity.getStatus().name(),
                entity.getFeaturedUntil() != null && entity.getFeaturedUntil().isAfter(Instant.now())
        );
    }
}
