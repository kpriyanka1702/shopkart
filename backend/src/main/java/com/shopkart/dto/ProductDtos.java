package com.shopkart.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ProductDtos {

    public record ProductRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description,
            @NotNull @DecimalMin("0.0") BigDecimal price,
            @DecimalMin("0.0") BigDecimal mrp,
            @NotBlank @Size(max = 100) String category,
            @Size(max = 500) String imageUrl,
            @NotNull @Min(0) Integer stock
    ) {}
}
