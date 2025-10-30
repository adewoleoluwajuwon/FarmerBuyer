package com.oau.farmerbuyer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "`order`", indexes = {
        @Index(name="idx_order_buyer_status", columnList = "buyer_id,order_status")
})
// ✅ FIX: Use @Builder.Default to ensure default values work with @Builder
@Builder
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="idempotency_key", length = 100)
    private String idempotencyKey;

    @ManyToOne(optional = false) @JoinColumn(name = "buyer_id")
    private AppUser buyer;

    @ManyToOne(optional = false) @JoinColumn(name = "farmer_id")
    private AppUser farmer;

    @ManyToOne(optional = false) @JoinColumn(name = "listing_id")
    private Listing listing;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityOrdered;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalNgn;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal platformFeeNgn = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalNgn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp private Instant updatedAt;
    private Instant deletedAt;

    public enum PaymentStatus { UNPAID, PAID, REFUNDED }
    public enum OrderStatus { PENDING, CONFIRMED, CANCELLED, FULFILLED }
}