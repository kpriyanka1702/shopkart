package com.shopkart.service;

import com.shopkart.dto.ProductDtos.ProductRequest;
import com.shopkart.exception.ApiException;
import com.shopkart.model.Product;
import com.shopkart.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> list(String category, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        if (StringUtils.hasText(search)) {
            return productRepository.search(search.trim(), pageable);
        }
        if (StringUtils.hasText(category)) {
            return productRepository.findByCategoryIgnoreCase(category, pageable);
        }
        return productRepository.findAll(pageable);
    }

    @Cacheable(value = "products", key = "#id")
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found: " + id));
    }

    @CacheEvict(value = "products", key = "#result.id")
    public Product create(ProductRequest req) {
        Product product = Product.builder()
                .name(req.name())
                .description(req.description())
                .price(req.price())
                .mrp(req.mrp())
                .category(req.category())
                .imageUrl(req.imageUrl())
                .stock(req.stock())
                .build();
        return productRepository.save(product);
    }

    @CacheEvict(value = "products", key = "#id")
    public Product update(Long id, ProductRequest req) {
        Product product = getById(id);
        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setMrp(req.mrp());
        product.setCategory(req.category());
        product.setImageUrl(req.imageUrl());
        product.setStock(req.stock());
        return productRepository.save(product);
    }

    /** Evicts a single product from the cache (e.g. after stock changes on checkout). */
    @CacheEvict(value = "products", key = "#id")
    public void evictProductCache(Long id) {
        // no-op: annotation handles the eviction
    }

    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw ApiException.notFound("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }
}
