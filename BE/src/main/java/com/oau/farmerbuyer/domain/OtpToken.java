package com.oau.farmerbuyer.domain;


import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "otp_token", indexes = {
        @Index(name="idx_otp_phone_exp", columnList = "phone_e164,expires_at")
})
public class OtpToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="phone_e164", nullable=false, length=20)
    private String phoneE164;

    @Column(name="code_hash", nullable=false, length=64)
    private byte[] codeHash;

    @Column(name="expires_at", nullable=false)
    private Instant expiresAt;

    @Column(nullable=false)
    private int attempts;

    private Instant consumedAt;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @PrePersist void pre() { createdAt = Instant.now(); }
}
