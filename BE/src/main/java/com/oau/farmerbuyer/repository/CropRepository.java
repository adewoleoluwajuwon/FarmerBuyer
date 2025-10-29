package com.oau.farmerbuyer.repository;

import com.oau.farmerbuyer.domain.Crop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CropRepository extends JpaRepository<Crop, Long> {
    Page<Crop> findByIsActiveTrue(Pageable pageable);
    Page<Crop> findByIsActiveTrueAndNameContainingIgnoreCase(String q, Pageable pageable);
    List<Crop> findTop20ByIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String q);
}
