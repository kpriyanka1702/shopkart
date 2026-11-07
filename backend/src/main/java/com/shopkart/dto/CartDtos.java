package com.shopkart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class CartDtos {

    public record AddToCartRequest(
            @NotNull Long productId,
            @NotNull @Min(1) Integer quantity
    ) {}

    public record CartItemResponse(
            Long id,
            Long productId,
            String productName,
            String imageUrl,
            BigDecimal price,
            Integer quantity,
            BigDecimal lineTotal
    ) {}

    public record CartResponse(
            List<CartItemResponse> items,
            BigDecimal total
    ) {}
}
