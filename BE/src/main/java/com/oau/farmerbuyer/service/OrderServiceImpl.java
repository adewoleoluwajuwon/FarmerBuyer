package com.oau.farmerbuyer.service;




import com.oau.farmerbuyer.domain.Order;
import com.oau.farmerbuyer.dto.OrderDtos;
import com.oau.farmerbuyer.repository.AppUserRepository;
import com.oau.farmerbuyer.repository.ListingRepository;
import com.oau.farmerbuyer.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service @RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepo;
    private final ListingRepository listingRepo;
    private final AppUserRepository userRepo;

    @Override @Transactional
    public OrderDtos.Response place(OrderDtos.Create dto, String idemKey) {
        var buyer = userRepo.findById(dto.buyerId()).orElseThrow();
        var listing = listingRepo.findById(dto.listingId()).orElseThrow();

        // If key present and a previous order exists → return it
        if (idemKey != null && !idemKey.isBlank()) {
            var existing = orderRepo.findByBuyerAndIdempotencyKey(buyer, idemKey);
            if (existing.isPresent()) {
                var o = existing.get();
                return new OrderDtos.Response(o.getId(), o.getOrderStatus().name(), o.getTotalNgn());
            }
        }

        // Create a new order
        var farmer = listing.getFarmer();
        var unitPrice = listing.getPricePerUnitNgn();
        var subtotal = unitPrice.multiply(dto.quantityOrdered());
        var fee = new BigDecimal("100.00");
        var total = subtotal.add(fee);

        var order = Order.builder()
                .buyer(buyer).farmer(farmer).listing(listing)
                .quantityOrdered(dto.quantityOrdered())
                .unitPriceSnapshot(unitPrice)
                .subtotalNgn(subtotal)
                .platformFeeNgn(fee)
                .totalNgn(total)
                .idempotencyKey((idemKey != null && !idemKey.isBlank()) ? idemKey : null)
                .build();

        try {
            var saved = orderRepo.save(order);
            return new OrderDtos.Response(saved.getId(), saved.getOrderStatus().name(), saved.getTotalNgn());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // race: another thread just created it → fetch and return
            var fallback = orderRepo.findByBuyerAndIdempotencyKey(buyer, idemKey).orElseThrow();
            return new OrderDtos.Response(fallback.getId(), fallback.getOrderStatus().name(), fallback.getTotalNgn());
        }
    }
}
