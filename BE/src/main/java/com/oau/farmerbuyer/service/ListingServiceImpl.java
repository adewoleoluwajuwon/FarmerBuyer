package com.oau.farmerbuyer.service;


import com.oau.farmerbuyer.domain.Listing;
import com.oau.farmerbuyer.dto.ListingDtos;
import com.oau.farmerbuyer.mapstruct.ListingMapper;
import com.oau.farmerbuyer.repository.AppUserRepository;
import com.oau.farmerbuyer.repository.CropRepository;
import com.oau.farmerbuyer.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import com.oau.farmerbuyer.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;


@Slf4j
@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final AppUserRepository userRepository;
    private final CropRepository cropRepository;
    private final ListingMapper mapper;

    private void assertOwnerOrAdmin(Listing listing) {
        Long me = SecurityUtils.currentUserId();
        if (me == null) throw new AccessDeniedException("Not authenticated");
        boolean admin = SecurityUtils.hasRole("ADMIN");
        if (!admin && !listing.getFarmer().getId().equals(me)) {
            throw new AccessDeniedException("Not your listing");
        }
    }


    @Override
    @Transactional
    public ListingDtos.Response create(ListingDtos.Create dto) {
        log.debug("Creating listing: {}", dto.title());

        var farmer = userRepository.findById(dto.farmerId())
                .orElseThrow(() -> new RuntimeException("Farmer not found with id: " + dto.farmerId()));

        var crop = cropRepository.findById(dto.cropId())
                .orElseThrow(() -> new RuntimeException("Crop not found with id: " + dto.cropId()));

        var listing = mapper.toEntity(dto);
        listing.setFarmer(farmer);
        listing.setCrop(crop);

        var saved = listingRepository.save(listing);
        log.info("Created listing with id: {}", saved.getId());

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ListingDtos.Response update(Long id, ListingDtos.Update dto) {
        log.debug("Updating listing id: {}", id);

        var listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found with id: " + id));

        assertOwnerOrAdmin(listing);

        mapper.updateEntity(listing, dto);
        var saved = listingRepository.save(listing);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingDtos.Response findById(Long id) {
        var listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found with id: " + id));
        return mapper.toResponse(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingDtos.Response> findActive(Pageable pageable) {
        return listingRepository.findByStatusAndDeletedAtIsNull(Listing.Status.ACTIVE, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingDtos.Summary> findActiveWithFilters(Long cropId, String location, Pageable pageable) {
        return listingRepository.findActiveWithFilters(cropId, location, pageable)
                .map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingDtos.Response> findByFarmer(Long farmerId, Pageable pageable) {
        return listingRepository.findByFarmerId(farmerId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found with id: " + id));

        assertOwnerOrAdmin(listing); // <— enforce

        listingRepository.deleteById(id);
        log.info("Hard deleted listing with id: {}", id);
    }


    @Override
    @Transactional
    public void softDelete(Long id) {
        var listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found with id: " + id));

        assertOwnerOrAdmin(listing);

        listing.setDeletedAt(Instant.now());
        listing.setStatus(Listing.Status.ARCHIVED);
        listingRepository.save(listing);

        log.info("Soft deleted listing with id: {}", id);
    }
}