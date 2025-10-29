package com.oau.farmerbuyer.repository;


import com.oau.farmerbuyer.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByPhoneE164(String phoneE164);
}
