package com.shopkart.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Selling price (what the customer pays). */
    @Column(nullable = false)
    private BigDecimal price;

    /** Original / list price (M.R.P). When greater than price, the difference is the discount. */
    private BigDecimal mrp;

    @Column(nullable = false)
    private String category;

    private String imageUrl;

    @Column(nullable = false)
    private Integer stock;
}
