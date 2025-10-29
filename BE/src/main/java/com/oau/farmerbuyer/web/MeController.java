// src/main/java/com/oau/farmerbuyer/web/MeController.java
package com.oau.farmerbuyer.web;

import com.oau.farmerbuyer.dto.UserDtos;

import com.oau.farmerbuyer.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MeController {
    private final AppUserRepository users;

    @GetMapping
    public UserDtos.MeResp me(Authentication auth) {
        Long id = (Long) auth.getPrincipal();
        var u = users.findById(id).orElseThrow();
        return new UserDtos.MeResp(u.getId(), u.getFullName(), u.getPhoneE164(), u.getRole().name());
    }

    @PutMapping
    public UserDtos.MeResp update(Authentication auth, @RequestBody UserDtos.MeUpdate dto) {
        Long id = (Long) auth.getPrincipal();
        var u = users.findById(id).orElseThrow();
        if (dto.fullName() != null && !dto.fullName().isBlank()) u.setFullName(dto.fullName());
        users.save(u);
        return new UserDtos.MeResp(u.getId(), u.getFullName(), u.getPhoneE164(), u.getRole().name());
    }
}
