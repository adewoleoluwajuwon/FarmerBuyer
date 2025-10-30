
package com.oau.farmerbuyer.repository;

import com.oau.farmerbuyer.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** Finds a payment by PSP and its provider reference (entity field: providerRef). */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByProviderAndProviderRef(String provider, String providerRef);
}
