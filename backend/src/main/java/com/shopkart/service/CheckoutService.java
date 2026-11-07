package com.shopkart.service;

import com.shopkart.dto.PaymentDtos.CheckoutResponse;
import com.shopkart.exception.ApiException;
import com.shopkart.model.Order;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Coordinates checkout with a Redis distributed lock, then creates a pending
 * order plus a payment intent. The lock (per user) prevents duplicate orders
 * from double-clicks or parallel tabs.
 */
@Service
public class CheckoutService {

    private static final Duration CHECKOUT_LOCK_TTL = Duration.ofSeconds(10);

    private final RedisLockService lockService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    public CheckoutService(RedisLockService lockService,
                           OrderService orderService,
                           PaymentService paymentService) {
        this.lockService = lockService;
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    public CheckoutResponse checkout(Long userId) {
        String lockKey = "lock:checkout:" + userId;
        String token = lockService.tryAcquire(lockKey, CHECKOUT_LOCK_TTL);
        if (token == null) {
            throw ApiException.conflict("A checkout is already in progress. Please wait.");
        }
        try {
            Order pending = orderService.createPendingOrder(userId);
            return paymentService.createIntent(pending);
        } finally {
            lockService.release(lockKey, token);
        }
    }
}
