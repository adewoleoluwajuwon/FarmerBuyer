package com.oau.farmerbuyer.service;



import com.oau.farmerbuyer.dto.ListingDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingService {

    ListingDtos.Response create(ListingDtos.Create dto);
    ListingDtos.Response update(Long id, ListingDtos.Update dto);
    ListingDtos.Response findById(Long id);
    Page<ListingDtos.Response> findActive(Pageable pageable);
    Page<ListingDtos.Summary> findActiveWithFilters(Long cropId, String location, Pageable pageable);
    Page<ListingDtos.Response> findByFarmer(Long farmerId, Pageable pageable);
    void delete(Long id);
    void softDelete(Long id);
}