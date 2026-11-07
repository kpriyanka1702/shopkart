package com.shopkart.service;

import com.shopkart.dto.CartDtos.*;
import com.shopkart.exception.ApiException;
import com.shopkart.model.CartItem;
import com.shopkart.model.Product;
import com.shopkart.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    public CartService(CartItemRepository cartItemRepository, ProductService productService) {
        this.cartItemRepository = cartItemRepository;
        this.productService = productService;
    }

    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest req) {
        Product product = productService.getById(req.productId());
        if (product.getStock() < req.quantity()) {
            throw ApiException.badRequest("Insufficient stock for " + product.getName());
        }
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, req.productId())
                .orElseGet(() -> CartItem.builder()
                        .userId(userId)
                        .product(product)
                        .quantity(0)
                        .build());
        item.setQuantity(item.getQuantity() + req.quantity());
        cartItemRepository.save(item);
        return getCart(userId);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long productId, int quantity) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ApiException.notFound("Item not in cart"));
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
        return getCart(userId);
    }

    @Transactional
    public CartResponse removeFromCart(Long userId, Long productId) {
        cartItemRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(cartItemRepository::delete);
        return getCart(userId);
    }

    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        List<CartItemResponse> responses = items.stream()
                .map(this::toResponse)
                .toList();
        BigDecimal total = responses.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(responses, total);
    }

    private CartItemResponse toResponse(CartItem item) {
        Product p = item.getProduct();
        BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(), p.getId(), p.getName(), p.getImageUrl(),
                p.getPrice(), item.getQuantity(), lineTotal);
    }
}
