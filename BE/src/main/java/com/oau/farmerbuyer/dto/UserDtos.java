package com.oau.farmerbuyer.dto;

// src/main/java/com/oau/farmerbuyer/dto/UserDtos.java

public class UserDtos {
    public record MeResp(Long id, String fullName, String phoneE164, String role) {}
    public record MeUpdate(String fullName) {}
}
