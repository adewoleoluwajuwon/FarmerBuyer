package com.oau.farmerbuyer.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitResponse {
    private String authorizationUrl;
    private String reference; // Return Paystack reference you initialize with
}
