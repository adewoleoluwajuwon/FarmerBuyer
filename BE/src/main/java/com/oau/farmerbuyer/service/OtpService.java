package com.oau.farmerbuyer.service;




import com.oau.farmerbuyer.domain.AppUser;
import com.oau.farmerbuyer.domain.OtpToken;
import com.oau.farmerbuyer.repository.AppUserRepository;
import com.oau.farmerbuyer.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Service @RequiredArgsConstructor
public class OtpService {
    private final OtpTokenRepository otpRepo;
    private final AppUserRepository userRepo;
    private final TokenService tokenService;

    @Value("${app.otp.ttlSeconds}") private int ttlSeconds;
    @Value("${app.otp.maxRequestsPerHour}") private int maxRequestsPerHour;
    @Value("${app.otp.maxVerifyAttempts}") private int maxVerifyAttempts;
    @Value("${app.otp.secret}") private String otpSecret;
    @Value("${app.otp.echoInResponse:false}") private boolean echoInResponse;

    private static final Random RND = new Random();

    private byte[] hash(String phone, String code) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            md.update((phone + "|" + code + "|" + otpSecret).getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public String requestOtp(String phone) {
        var hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        if (otpRepo.countRequestsSince(phone, hourAgo) >= maxRequestsPerHour) {
            throw new IllegalArgumentException("Too many OTP requests. Try again later.");
        }
        var code = String.format("%06d", RND.nextInt(1_000_000));
        var token = OtpToken.builder()
                .phoneE164(phone)
                .codeHash(hash(phone, code))
                .expiresAt(Instant.now().plusSeconds(ttlSeconds))
                .attempts(0)
                .build();
        otpRepo.save(token);

        // TODO integrate WhatsApp/SMS provider here.
        // For local/dev, return the code (guarded by echoInResponse).
        return echoInResponse ? code : "SENT";
    }

    @Transactional
    public String verifyOtp(String phone, String code) {
        var latest = otpRepo.findTopByPhoneE164OrderByIdDesc(phone)
                .orElseThrow(() -> new IllegalArgumentException("No OTP requested"));

        if (latest.getConsumedAt() != null || Instant.now().isAfter(latest.getExpiresAt())) {
            throw new IllegalArgumentException("OTP expired");
        }
        if (latest.getAttempts() >= maxVerifyAttempts) {
            throw new IllegalArgumentException("Too many attempts");
        }

        latest.setAttempts(latest.getAttempts() + 1);
        var ok = MessageDigest.isEqual(latest.getCodeHash(), hash(phone, code));
        if (!ok) {
            // persist attempt increment
            otpRepo.save(latest);
            throw new IllegalArgumentException("Invalid code");
        }

        latest.setConsumedAt(Instant.now());
        otpRepo.save(latest);

        // Upsert user (verified)
        var user = userRepo.findByPhoneE164(phone)
                .orElseGet(() -> userRepo.save(AppUser.builder()
                        .phoneE164(phone).fullName(null).role(AppUser.Role.BUYER).isVerified(true).build()));
        if (!user.isVerified()) { user.setVerified(true); userRepo.save(user); }

        // Issue JWT (you can protect routes later)
        return tokenService.issueJwt(user.getId(), phone, user.getRole().name());
    }
}
