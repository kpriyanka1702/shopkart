package com.shopkart.repository;

import com.shopkart.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    /**
     * Live search: prioritises names/categories that START WITH the query
     * (e.g. "la" -> "Laptop 14-inch", "Laptop Backpack"), and also matches
     * a name that CONTAINS the query as a fallback.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT(:q, '%'))
               OR LOWER(p.category) LIKE LOWER(CONCAT(:q, '%'))
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY
               CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT(:q, '%')) THEN 0
                    WHEN LOWER(p.category) LIKE LOWER(CONCAT(:q, '%')) THEN 1
                    ELSE 2 END,
               p.name
            """)
    Page<Product> search(@Param("q") String q, Pageable pageable);
}
