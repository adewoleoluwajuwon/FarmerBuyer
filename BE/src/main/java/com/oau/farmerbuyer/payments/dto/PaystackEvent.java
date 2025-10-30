package com.oau.farmerbuyer.payments.dto;

import lombok.Data;

@Data
public class PaystackEvent {
    private String event;
    private Data data;

    @lombok.Data
    public static class Data {
        private String reference;
        private int amount;      // kobo
        private String currency; // e.g. NGN
    }
}
