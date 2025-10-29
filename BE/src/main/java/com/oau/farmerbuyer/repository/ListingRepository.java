package com.oau.farmerbuyer.repository;



import com.oau.farmerbuyer.domain.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Page<Listing> findByStatus(Listing.Status status, Pageable pageable);

    Page<Listing> findByStatusAndDeletedAtIsNull(Listing.Status status, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE l.farmer.id = :farmerId AND l.deletedAt IS NULL")
    Page<Listing> findByFarmerId(@Param("farmerId") Long farmerId, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE l.crop.id = :cropId AND l.status = 'ACTIVE' AND l.deletedAt IS NULL")
    Page<Listing> findActiveByCropId(@Param("cropId") Long cropId, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND l.deletedAt IS NULL " +
            "AND (:cropId IS NULL OR l.crop.id = :cropId) " +
            "AND (:location IS NULL OR LOWER(l.locationText) LIKE LOWER(CONCAT('%', :location, '%')))")
    Page<Listing> findActiveWithFilters(@Param("cropId") Long cropId,
                                        @Param("location") String location,
                                        Pageable pageable);
}
