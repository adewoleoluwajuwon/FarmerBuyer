// src/main/java/com/oau/farmerbuyer/service/OrderServiceImpl.java
package com.oau.farmerbuyer.service;

import com.oau.farmerbuyer.domain.Order;
import com.oau.farmerbuyer.dto.OrderDtos;
import com.oau.farmerbuyer.repository.AppUserRepository;
import com.oau.farmerbuyer.repository.ListingRepository;
import com.oau.farmerbuyer.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepo;
    private final ListingRepository listingRepo;
    private final AppUserRepository userRepo;

    /**
     * New canonical path: create a PENDING order (idempotent by header key).
     * - buyerId comes from Authentication (controller)
     * - req contains listingId, quantityOrdered, deliveryOption, addressId, notes, idemKey
     * - service calculates totals from a fresh price snapshot
     */
    @Override
    @Transactional
    public OrderDtos.Response createPendingOrder(Long buyerId, OrderDtos.Create req, String idemKey) {
        final var buyer = userRepo.findById(buyerId).orElseThrow();
        final var listing = listingRepo.findById(req.listingId()).orElseThrow();

        // Idempotency: if key present and an order already exists for this buyer/key, return it.
        if (idemKey != null && !idemKey.isBlank()) {
            final var existing = orderRepo.findByBuyerAndIdempotencyKey(buyer, idemKey);
            if (existing.isPresent()) {
                final var o = existing.get();
                return new OrderDtos.Response(o.getId(), o.getOrderStatus().name(), o.getTotalNgn());
            }
        }

        // Pricing snapshot (server-side)
        final BigDecimal unitPrice = listing.getPricePerUnitNgn();
        final BigDecimal qty = req.quantityOrdered();
        final BigDecimal subtotal = unitPrice.multiply(qty);

        // Simple platform fee for now (replace with fee service when ready)
        final BigDecimal platformFee = new BigDecimal("100.00");
        final BigDecimal total = subtotal.add(platformFee);

        // Build order in PENDING (explicitly set status if your entity doesn't default it)
        final var order = Order.builder()
                .buyer(buyer)
                .farmer(listing.getFarmer())
                .listing(listing)
                .quantityOrdered(qty)
                .unitPriceSnapshot(unitPrice)
                .subtotalNgn(subtotal)
                .platformFeeNgn(platformFee)
                .totalNgn(total)
                .idempotencyKey((idemKey != null && !idemKey.isBlank()) ? idemKey : null)
                // .orderStatus(OrderStatus.PENDING) // uncomment if not defaulted in entity
                // .paymentStatus(PaymentStatus.UNPAID) // if you track payment status separately
                .build();

        try {
            final var saved = orderRepo.save(order);
            return new OrderDtos.Response(saved.getId(), saved.getOrderStatus().name(), saved.getTotalNgn());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Race on idempotency unique constraint → fetch and return existing
            final var fallback = orderRepo.findByBuyerAndIdempotencyKey(buyer, idemKey).orElseThrow();
            return new OrderDtos.Response(fallback.getId(), fallback.getOrderStatus().name(), fallback.getTotalNgn());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDtos.Response> myOrders(Long buyerId, Pageable pageable) {
        return orderRepo.findByBuyer_IdOrderByCreatedAtDesc(buyerId, pageable)
                .map(o -> new OrderDtos.Response(o.getId(), o.getOrderStatus().name(), o.getTotalNgn()));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDtos.Response getOne(Long id, Long buyerId) {
        final var o = orderRepo.findByIdAndBuyer_Id(id, buyerId).orElseThrow();
        return new OrderDtos.Response(o.getId(), o.getOrderStatus().name(), o.getTotalNgn());
    }
}
