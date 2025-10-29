package com.oau.farmerbuyer.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oau.farmerbuyer.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${payments.paystack.secret}")
    String paystackSecret;

    @Value("${payments.flutterwave.hash}")
    String flutterwaveHash;

    @PostMapping("/paystack/webhook")
    public ResponseEntity<String> paystack(HttpServletRequest request) throws Exception {
        // Explicit String type avoids readTree(...) ambiguity
        String raw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

        String sig = request.getHeader("x-paystack-signature");
        if (!verifyPaystack(raw, sig)) {
            return ResponseEntity.status(401).body("invalid signature");
        }

        JsonNode evt = om.readTree(raw); // unambiguous: String overload
        paymentService.handlePaystack(evt);
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/flutterwave/webhook")
    public ResponseEntity<String> flutterwave(@RequestBody String raw,
                                              @RequestHeader(value = "verif-hash", required = false) String verif) throws Exception {
        if (verif == null || !verif.equals(flutterwaveHash)) {
            return ResponseEntity.status(401).body("invalid signature");
        }
        JsonNode evt = om.readTree(raw); // String overload
        paymentService.handleFlutterwave(evt);
        return ResponseEntity.ok("ok");
    }

    private boolean verifyPaystack(String raw, String headerSig) throws Exception {
        if (headerSig == null) return false;
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(paystackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        String computed = bytesToHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        return computed.equalsIgnoreCase(headerSig);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
