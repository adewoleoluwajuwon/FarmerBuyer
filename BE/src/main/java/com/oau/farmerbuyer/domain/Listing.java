package com.oau.farmerbuyer.domain;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "listing", indexes = {
        @Index(name="idx_listing_status_crop", columnList = "status,crop_id")
})
public class Listing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "farmer_id")
    private AppUser farmer;

    @ManyToOne(optional = false) @JoinColumn(name = "crop_id")
    private Crop crop;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Crop.Unit unit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityAvailable;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal minOrderQty = BigDecimal.ONE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerUnitNgn;

    private String locationText;

    @Column(precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(precision = 11, scale = 8)
    private BigDecimal lng;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.ACTIVE;

    private Instant featuredUntil;
    private Integer photosCount = 0;

    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp private Instant updatedAt;
    private Instant deletedAt;

    public enum Status { DRAFT, ACTIVE, SOLD_OUT, ARCHIVED }
}
