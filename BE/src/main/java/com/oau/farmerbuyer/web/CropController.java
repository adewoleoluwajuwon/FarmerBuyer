package com.oau.farmerbuyer.web;

import com.oau.farmerbuyer.domain.Crop;
import com.oau.farmerbuyer.dto.CropDtos;
import com.oau.farmerbuyer.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crops")
@RequiredArgsConstructor
public class CropController {
    private final CropRepository repo;

    @GetMapping
    public Page<CropDtos.Response> list(@RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size);
        Page<Crop> p = (q == null || q.isBlank())
                ? repo.findByIsActiveTrue(pageable)
                : repo.findByIsActiveTrueAndNameContainingIgnoreCase(q, pageable);
        var mapped = p.getContent().stream().map(this::map).toList();
        return new PageImpl<>(mapped, pageable, p.getTotalElements());
    }

    @GetMapping("/autocomplete")
    public List<CropDtos.Response> autocomplete(@RequestParam String q,
                                                @RequestParam(defaultValue = "10") int limit) {
        int capped = Math.max(1, Math.min(limit, 20));
        return repo.findTop20ByIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(q)
                .stream()
                .limit(capped)
                .map(this::map)
                .toList();
    }

    private CropDtos.Response map(Crop c) {
        return new CropDtos.Response(
                c.getId(),
                c.getName(),
                c.getCategory(),
                c.getDefaultUnit().name(),
                c.isActive() // ← key change
        );
    }
}
