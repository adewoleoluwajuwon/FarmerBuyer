package com.oau.farmerbuyer.service;


import com.oau.farmerbuyer.dto.OrderDtos;

public interface OrderService {
    OrderDtos.Response place(OrderDtos.Create dto, String idemKey);
}
