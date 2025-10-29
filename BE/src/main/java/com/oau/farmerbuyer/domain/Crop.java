package com.oau.farmerbuyer.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "crop")
public class Crop {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120, unique = true)
    private String name;

    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Unit defaultUnit = Unit.KG;

    @Column(nullable = false)
    private boolean isActive = true;

    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp private Instant updatedAt;

    public enum Unit { KG, BAG, CRATE, BUNCH, TON }
}
