package com.oau.farmerbuyer.payments.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentInitRequest {
    private Long orderId;
}
