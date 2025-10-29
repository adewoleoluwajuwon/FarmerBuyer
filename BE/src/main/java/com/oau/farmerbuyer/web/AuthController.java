package com.oau.farmerbuyer.web;



import com.fasterxml.jackson.annotation.JsonProperty;

import com.oau.farmerbuyer.service.OtpService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final OtpService otpService;

    public record OtpRequest(@NotBlank String phone) {}
    public record OtpVerify(@NotBlank String phone, @NotBlank String code) {}
    public record OtpRequested(String status, String devCode) {}
    public record TokenResponse(@JsonProperty("token") String token) {}

    @PostMapping("/otp/request")
    public ResponseEntity<OtpRequested> request(@RequestBody OtpRequest req) {
        var dev = otpService.requestOtp(req.phone());
        return ResponseEntity.ok(new OtpRequested("SENT", dev.equals("SENT") ? null : dev));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<TokenResponse> verify(@RequestBody OtpVerify req) {
        var jwt = otpService.verifyOtp(req.phone(), req.code());
        return ResponseEntity.ok(new TokenResponse(jwt));
    }
}
