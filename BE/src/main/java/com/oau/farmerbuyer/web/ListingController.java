package com.oau.farmerbuyer.web;




import com.oau.farmerbuyer.dto.ListingDtos;
import com.oau.farmerbuyer.security.SecurityUtils;
import com.oau.farmerbuyer.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;


import org.springframework.security.access.prepost.PreAuthorize;



@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @PreAuthorize("hasRole('FARMER')")
    @PostMapping
    public ResponseEntity<ListingDtos.Response> create(@Valid @RequestBody ListingDtos.Create dto) {
        var response = listingService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('FARMER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ListingDtos.Response> update(@PathVariable Long id,
                                                       @Valid @RequestBody ListingDtos.Update dto) {
        var response = listingService.update(id, dto);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<ListingDtos.Response> findById(@PathVariable Long id) {
        var response = listingService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ListingDtos.Summary>> findActive(
            @RequestParam(required = false) Long cropId,
            @RequestParam(required = false) String location,
            @PageableDefault(size = 20) Pageable pageable) {

        var response = listingService.findActiveWithFilters(cropId, location, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<Page<ListingDtos.Response>> findByFarmer(@PathVariable Long farmerId,
                                                                   @PageableDefault Pageable pageable) {
        var response = listingService.findByFarmer(farmerId, pageable);
        return ResponseEntity.ok(response);
    }



    @PreAuthorize("hasAnyRole('FARMER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        listingService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/farmer/me")
    @PreAuthorize("hasAnyRole('FARMER','ADMIN')")
    public ResponseEntity<Page<ListingDtos.Response>> myListings(
            @PageableDefault Pageable pageable,
            Authentication auth
    ) {
        Long farmerId = SecurityUtils.currentUserId(auth);
        var response = listingService.findByFarmer(farmerId, pageable);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('FARMER','ADMIN')")
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable Long id) {
        listingService.delete(id);
        return ResponseEntity.noContent().build();
    }


}