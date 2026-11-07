package com.shopkart.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderDtos {

    public record OrderItemResponse(
            Long productId,
            String productName,
            BigDecimal price,
            Integer quantity
    ) {}

    public record OrderResponse(
            Long id,
            BigDecimal total,
            String status,
            Instant createdAt,
            List<OrderItemResponse> items
    ) {}
}
