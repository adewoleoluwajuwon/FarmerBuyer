package com.oau.farmerbuyer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oau.farmerbuyer.domain.Order;
import com.oau.farmerbuyer.domain.Payment;
import com.oau.farmerbuyer.payments.dto.PaymentInitResponse;
import com.oau.farmerbuyer.payments.dto.PaystackEvent;
import com.oau.farmerbuyer.payments.dto.PaystackInitResponse;
import com.oau.farmerbuyer.repository.AppUserRepository;
import com.oau.farmerbuyer.repository.OrderRepository;
import com.oau.farmerbuyer.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository payRepo;
    private final OrderRepository orderRepo;
    private final AppUserRepository userRepo;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${payments.paystack.secret:${payments.paystack.secretKey:}}")
    private String paystackSecret;

    @Value("${payments.paystack.baseUrl:https://api.paystack.co}")
    private String paystackBaseUrl;

    @Value("${app.baseUrl:http://localhost:5173}")
    private String appBaseUrl;

    @Value("${payments.currency:NGN}")
    private String defaultCurrency;

    // === Plain MVC client ===
    private RestTemplate rest() {
        return new RestTemplate();
    }

    // === Helpers ===
    private String providerKey() { return "PAYSTACK"; }

    private int ngnToKobo(BigDecimal ngn) {
        return ngn.movePointRight(2).intValueExact();
    }

    private BigDecimal resolveOrderTotal(Order order) {
        try {
            Method mTotal = Order.class.getMethod("getTotalNgn");
            Object v = mTotal.invoke(order);
            if (v instanceof BigDecimal bd) return bd;

            Method mSub = Order.class.getMethod("getSubtotalNgn");
            Method mFee = Order.class.getMethod("getPlatformFeeNgn");
            Object sub = mSub.invoke(order);
            Object fee = mFee.invoke(order);
            if (sub instanceof BigDecimal s && fee instanceof BigDecimal f) return s.add(f);
        } catch (Exception ignore) { }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot resolve order total");
    }

    private String safeOrderNumber(Order order) {
        try {
            Method m = Order.class.getMethod("getNumber");
            Object val = m.invoke(order);
            return (val != null) ? String.valueOf(val) : String.valueOf(order.getId());
        } catch (Exception ignore) {
            return String.valueOf(order.getId());
        }
    }

    /** Try AppUser.getEmail()/getUsername()/getPhone(), else fallback. */
    private String resolveBuyerEmail(Long buyerId) {
        return userRepo.findById(buyerId).map(u -> {
            try {
                Method getEmail = u.getClass().getMethod("getEmail");
                Object email = getEmail.invoke(u);
                if (email != null && !String.valueOf(email).isBlank()) return String.valueOf(email);
            } catch (Exception ignore) {}
            try {
                Method getUsername = u.getClass().getMethod("getUsername");
                Object un = getUsername.invoke(u);
                if (un != null && String.valueOf(un).contains("@")) return String.valueOf(un);
            } catch (Exception ignore) {}
            try {
                Method getPhone = u.getClass().getMethod("getPhone");
                Object p = getPhone.invoke(u);
                if (p != null && !String.valueOf(p).isBlank()) return String.valueOf(p) + "@example.com";
            } catch (Exception ignore) {}
            return "buyer@example.com";
        }).orElse("buyer@example.com");
    }

    private String writeJson(Object o) {
        try { return om.writeValueAsString(o); } catch (Exception e) { return null; }
    }

    private boolean verifySignature(String raw, String sig) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(paystackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] h = mac.doFinal(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(h.length * 2);
            for (byte b : h) sb.append(String.format("%02x", b));
            String hex = sb.toString();
            return hex.equalsIgnoreCase(sig);
        } catch (Exception e) {
            log.warn("HMAC verification failed: {}", e.getMessage());
            return false;
        }
    }

    private Long tryExtractOrderId(String ref) {
        try {
            if (ref.startsWith("order-")) {
                String rest = ref.substring(6);
                int dash = rest.indexOf('-');
                return Long.parseLong(dash > 0 ? rest.substring(0, dash) : rest);
            }
            return Long.parseLong(ref);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse orderId from reference: " + ref, e);
        }
    }

    // ==================== INIT ====================

    @Transactional
    public PaymentInitResponse initPaystackPayment(Long buyerId, Long orderId) {
        // Guard ownership
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getBuyer() == null || !order.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to buyer");
        }

        String reference = "order-" + order.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);

        int amountKobo = ngnToKobo(resolveOrderTotal(order));
        String email = resolveBuyerEmail(buyerId);
        String currency = Optional.ofNullable(defaultCurrency).orElse("NGN");
        String callbackUrl = appBaseUrl + "/buyer/payment/return?orderId=" + order.getId() + "&ref=" + reference;

        Map<String, Object> payload = Map.of(
                "amount", amountKobo,
                "email", email,
                "reference", reference,
                "currency", currency,
                "callback_url", callbackUrl,
                "metadata", Map.of(
                        "orderId", String.valueOf(order.getId()),
                        "number", safeOrderNumber(order),
                        "buyerId", String.valueOf(buyerId)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(paystackSecret);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        String url = paystackBaseUrl + "/transaction/initialize";

        ResponseEntity<PaystackInitResponse> resp =
                rest().exchange(url, HttpMethod.POST, entity, PaystackInitResponse.class);

        PaystackInitResponse ps = resp.getBody();
        if (resp.getStatusCode().isError() || ps == null || !ps.isStatus() || ps.getData() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Paystack init failed: " + (ps != null ? ps.getMessage() : resp.getStatusCode().toString()));
        }

        // Persist INITIATED payment
        Payment p = Payment.builder()
                .order(order)
                .provider(Payment.Provider.valueOf(providerKey()))
                .providerRef(ps.getData().getReference())
                .amountNgn(resolveOrderTotal(order))
                .status(Payment.Status.INITIATED) // or PENDING
                .rawPayloadJson(writeJson(ps))
                .build();
        payRepo.save(p);

        // Your FE contract
        return new PaymentInitResponse(
                ps.getData().getAuthorization_url(),
                ps.getData().getReference()
        );
    }

    // ==================== WEBHOOK ====================

    @Transactional
    public void handlePaystackWebhook(String signature, String rawBody) {
        if (!verifySignature(rawBody, signature)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid signature");
        }

        PaystackEvent evt;
        try {
            evt = om.readValue(rawBody, PaystackEvent.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook JSON", e);
        }

        if (!"charge.success".equalsIgnoreCase(evt.getEvent())) {
            log.debug("Ignoring Paystack event: {}", evt.getEvent());
            return;
        }

        String ref = evt.getData().getReference();
        Payment pay = payRepo.findByProviderAndProviderRef(providerKey(), ref).orElseGet(() -> {
            Long orderId = tryExtractOrderId(ref);
            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found for ref"));
            return Payment.builder()
                    .order(order)
                    .provider(Payment.Provider.valueOf(providerKey()))
                    .providerRef(ref)
                    .amountNgn(resolveOrderTotal(order))
                    .status(Payment.Status.INITIATED)
                    .build();
        });

        // Idempotency
        if (pay.getStatus() == Payment.Status.SUCCESS) return;

        Order order = pay.getOrder();

        // Guard amount & currency
        int amountKobo = evt.getData().getAmount();
        String currency = evt.getData().getCurrency();
        if (!Optional.ofNullable(currency).orElse(defaultCurrency).equalsIgnoreCase(defaultCurrency)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency mismatch");
        }
        if (amountKobo != ngnToKobo(resolveOrderTotal(order))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount mismatch");
        }

        // Transition → success
        pay.setStatus(Payment.Status.SUCCESS);
        pay.setPaidAt(Instant.now());
        pay.setRawPayloadJson(rawBody);
        payRepo.save(pay);

        order.setPaymentStatus(Order.PaymentStatus.PAID);
        // optionally: order.setOrderStatus(Order.OrderStatus.CONFIRMED);
        orderRepo.save(order);
    }
}
