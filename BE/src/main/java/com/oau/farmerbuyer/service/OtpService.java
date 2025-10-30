package com.oau.farmerbuyer.service;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {
    private final OtpTokenRepository otpRepo;
    private final AppUserRepository userRepo;
    private final TokenService tokenService;

    @Value("${app.otp.ttlSeconds}") private int ttlSeconds;
    @Value("${app.otp.maxRequestsPerHour}") private int maxRequestsPerHour;
    @Value("${app.otp.maxVerifyAttempts}") private int maxVerifyAttempts;
    @Value("${app.otp.secret}") private String otpSecret;
    @Value("${app.otp.echoInResponse:false}") private boolean echoInResponse;

    private static final java.security.SecureRandom RND = new java.security.SecureRandom();

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
        final String p = phone; // normalize to E.164 upstream if needed
        var hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

        if (otpRepo.countRequestsSince(p, hourAgo) >= maxRequestsPerHour) {
            throw new IllegalArgumentException("Too many OTP requests. Try again later.");
        }

        var code = String.format("%06d", RND.nextInt(1_000_000));
        var token = OtpToken.builder()
                .phoneE164(p)
                .codeHash(hash(p, code))
                .expiresAt(Instant.now().plusSeconds(ttlSeconds))
                .attempts(0)
                .build();

        // Only log OTP in dev when echoing back (never at INFO in prod)
        if (echoInResponse) {
            log.debug("Generated OTP for {} is: {}", p, code);
        }

        otpRepo.save(token);

        // TODO: integrate SMS/WhatsApp provider. For dev, return the code if echo enabled.
        return echoInResponse ? code : "SENT";
    }

    @Transactional
    public String verifyOtp(String phone, String code) {
        final String p = phone; // normalize upstream if needed

        var latest = otpRepo.findTopByPhoneE164OrderByIdDesc(p)
                .orElseThrow(() -> new IllegalArgumentException("No OTP requested"));

        if (latest.getConsumedAt() != null || Instant.now().isAfter(latest.getExpiresAt())) {
            throw new IllegalArgumentException("OTP expired");
        }
        if (latest.getAttempts() >= maxVerifyAttempts) {
            throw new IllegalArgumentException("Too many attempts");
        }

        var ok = MessageDigest.isEqual(latest.getCodeHash(), hash(p, code));
        if (!ok) {
            // increment ONLY on failure
            latest.setAttempts(latest.getAttempts() + 1);
            otpRepo.save(latest);
            throw new IllegalArgumentException("Invalid code");
        }

        latest.setConsumedAt(Instant.now());
        otpRepo.save(latest);

        // Upsert user (verified)
        var user = userRepo.findByPhoneE164(p)
                .orElseGet(() -> userRepo.save(
                        AppUser.builder()
                                .phoneE164(p)
                                .fullName(null)
                                .role(AppUser.Role.BUYER)
                                .isVerified(true)
                                .build()
                ));
        if (!user.isVerified()) {
            user.setVerified(true);
            userRepo.save(user);
        }

        // Issue JWT
        return tokenService.issueJwt(user.getId(), p, user.getRole().name());
    }
}
