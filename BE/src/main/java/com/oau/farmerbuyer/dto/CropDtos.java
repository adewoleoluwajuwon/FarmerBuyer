package com.oau.farmerbuyer.dto;

public class CropDtos {
    public record Response(Long id, String name, String category, String defaultUnit, boolean isActive) {}
}
