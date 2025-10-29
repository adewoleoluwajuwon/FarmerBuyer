package com.oau.farmerbuyer.dto;

import java.math.BigDecimal;

public class OrderDtos {
    public record Create(Long buyerId, Long listingId, BigDecimal quantityOrdered) {}
    public record Response(Long id, String status, BigDecimal totalNgn) {}
}
