package com.oau.farmerbuyer.payments.dto;

import lombok.Data;

@Data
public class PaystackInitResponse {
    private boolean status;
    private String message;
    private Data data;

    @lombok.Data
    public static class Data {
        private String authorization_url;
        private String access_code; // we won't return this to FE, but useful to log/store
        private String reference;
    }
}
