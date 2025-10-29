package com.oau.farmerbuyer.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.oau.farmerbuyer.domain.Payment;
import com.oau.farmerbuyer.domain.Order;
import com.oau.farmerbuyer.repository.PaymentRepository;
import com.oau.farmerbuyer.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void handlePaystack(JsonNode evt) {
        log.debug("Processing Paystack webhook: {}", evt.path("event").asText());

        var event = evt.path("event").asText(); // e.g., "charge.success"
        var data = evt.path("data");
        var reference = data.path("reference").asText();
        var amountKobo = data.path("amount").asLong(); // kobo
        var status = "charge.success".equals(event) ? "SUCCESS" : "FAILED";

        upsertPayment("PAYSTACK", reference, koboToNgn(amountKobo), status, data);
    }

    @Transactional
    public void handleFlutterwave(JsonNode evt) {
        log.debug("Processing Flutterwave webhook: {}", evt.path("status").asText());

        var status = evt.path("status").asText(); // "successful"
        var data = evt.path("data");
        var reference = data.path("tx_ref").asText();
        var amount = data.path("amount").decimalValue();

        upsertPayment("FLUTTERWAVE", reference,
                amount.setScale(2, RoundingMode.HALF_UP),
                "successful".equalsIgnoreCase(status) ? "SUCCESS" : "FAILED",
                data);
    }

    private BigDecimal koboToNgn(long kobo) {
        return new BigDecimal(kobo).movePointLeft(2);
    }

    private void upsertPayment(String provider, String providerRef, BigDecimal amount, String status, JsonNode payload) {
        try {
            // Check if payment already exists (idempotent)
            var existing = paymentRepository.findByProviderAndProviderRef(provider, providerRef);
            if (existing.isPresent()) {
                log.info("Payment already exists for provider: {} and reference: {}", provider, providerRef);
                return;
            }

            // Extract order ID from provider reference
            Long orderId = tryExtractOrderId(providerRef);
            var order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

            // Create payment record
            var payment = Payment.builder()
                    .order(order)
                    .provider(Payment.Provider.valueOf(provider))
                    .providerRef(providerRef)
                    .amountNgn(amount)
                    .status(mapStatus(status))
                    .paidAt("SUCCESS".equals(status) ? Instant.now() : null)
                    .rawPayloadJson(payload.toString())
                    .build();

            paymentRepository.save(payment);
            log.info("Payment saved: {} - {} - {}", provider, providerRef, status);

            // Update order payment status if successful
            if ("SUCCESS".equals(status)) {
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                orderRepository.save(order);
                log.info("Order {} payment status updated to PAID", orderId);
            }

        } catch (Exception e) {
            log.error("Error processing payment: provider={}, ref={}, error={}", provider, providerRef, e.getMessage(), e);
            throw new RuntimeException("Failed to process payment", e);
        }
    }

    private Payment.Status mapStatus(String status) {
        return switch (status) {
            case "SUCCESS" -> Payment.Status.SUCCESS;
            case "FAILED" -> Payment.Status.FAILED;
            default -> Payment.Status.INITIATED;
        };
    }

    private Long tryExtractOrderId(String ref) {
        // Expect "order-12345" or just "12345"
        try {
            if (ref.startsWith("order-")) {
                return Long.parseLong(ref.substring(6));
            }
            return Long.parseLong(ref);
        } catch (NumberFormatException e) {
            log.error("Cannot parse order ID from reference: {}", ref);
            throw new IllegalArgumentException("Cannot derive orderId from reference: " + ref, e);
        }
    }
}