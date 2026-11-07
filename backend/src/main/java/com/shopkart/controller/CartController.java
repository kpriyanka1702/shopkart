package com.shopkart.controller;

import com.shopkart.dto.CartDtos.*;
import com.shopkart.service.CartService;
import com.shopkart.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    public CartController(CartService cartService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        Long userId = currentUserService.require().getId();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping
    public ResponseEntity<CartResponse> add(@Valid @RequestBody AddToCartRequest request) {
        Long userId = currentUserService.require().getId();
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<CartResponse> update(@PathVariable Long productId,
                                               @RequestParam @Min(0) int quantity) {
        Long userId = currentUserService.require().getId();
        return ResponseEntity.ok(cartService.updateQuantity(userId, productId, quantity));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<CartResponse> remove(@PathVariable Long productId) {
        Long userId = currentUserService.require().getId();
        return ResponseEntity.ok(cartService.removeFromCart(userId, productId));
    }
}
