package com.oau.farmerbuyer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="payment", uniqueConstraints = @UniqueConstraint(columnNames={"provider","provider_ref"}))
public class Payment {
    public enum Provider { PAYSTACK, FLUTTERWAVE }
    public enum Status { INITIATED, SUCCESS, FAILED, REFUNDED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) @JoinColumn(name="order_id")
    private Order order;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private Provider provider;

    @Column(name="provider_ref", nullable=false, length=120)
    private String providerRef;

    @Column(name="amount_ngn", nullable=false, precision=12, scale=2)
    private BigDecimal amountNgn;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private Status status = Status.INITIATED;

    private Instant paidAt;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "raw_payload_json", columnDefinition = "LONGTEXT")
    private String rawPayloadJson;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @PrePersist void pre() { createdAt = Instant.now(); }

    @Column(name = "channel", length = 64)
    private String channel;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "gateway_fee", precision = 12, scale = 2)
    private BigDecimal gatewayFee;
}
