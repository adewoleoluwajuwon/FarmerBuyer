package com.oau.farmerbuyer.repository;



import com.oau.farmerbuyer.domain.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByPhoneE164OrderByIdDesc(String phone);

    @Query("""
    select count(o) from OtpToken o
     where o.phoneE164 = :phone and o.createdAt >= :since
  """)
    long countRequestsSince(String phone, Instant since);
}
