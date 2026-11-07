package com.shopkart.controller;

import com.shopkart.dto.OrderDtos.OrderResponse;
import com.shopkart.dto.PaymentDtos.CheckoutResponse;
import com.shopkart.service.CheckoutService;
import com.shopkart.service.CurrentUserService;
import com.shopkart.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final CheckoutService checkoutService;
    private final CurrentUserService currentUserService;

    public OrderController(OrderService orderService,
                           CheckoutService checkoutService,
                           CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.checkoutService = checkoutService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout() {
        Long userId = currentUserService.require().getId();
        return ResponseEntity.ok(checkoutService.checkout(userId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> myOrders() {
        Long userId = currentUserService.require().getId();
        return ResponseEntity.ok(orderService.getOrders(userId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        Long userId = currentUserService.require().getId();
        return ResponseEntity.ok(orderService.cancelOrder(id, userId));
    }
}
