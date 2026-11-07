package com.shopkart.service;

import com.shopkart.dto.OrderDtos.*;
import com.shopkart.exception.ApiException;
import com.shopkart.model.*;
import com.shopkart.repository.CartItemRepository;
import com.shopkart.repository.OrderRepository;
import com.shopkart.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        ProductService productService) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    /**
     * Creates a PENDING_PAYMENT order from the user's cart as a snapshot
     * (name/price/qty). Stock is NOT deducted and the cart is NOT cleared here —
     * that happens only after a successful payment in {@link #confirmOrder(Long)}.
     */
    @Transactional
    public Order createPendingOrder(Long userId) {
        List<CartItem> cart = cartItemRepository.findByUserId(userId);
        if (cart.isEmpty()) {
            throw ApiException.badRequest("Cart is empty");
        }

        Order order = Order.builder()
                .userId(userId)
                .status(Order.Status.PENDING_PAYMENT)
                .createdAt(Instant.now())
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart) {
            Product product = cartItem.getProduct();
            if (product.getStock() < cartItem.getQuantity()) {
                throw ApiException.badRequest("Insufficient stock for " + product.getName());
            }
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();
            order.getItems().add(orderItem);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotal(total);
        return orderRepository.save(order);
    }

    /**
     * Finalizes a paid order: re-validates and deducts stock, marks it PLACED,
     * and clears the user's cart. Runs in one transaction.
     */
    @Transactional
    public Order confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (order.getStatus() != Order.Status.PENDING_PAYMENT) {
            return order; // already processed — idempotent
        }
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.badRequest("Product no longer available"));
            if (product.getStock() < item.getQuantity()) {
                throw ApiException.badRequest("Insufficient stock for " + product.getName());
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
            productService.evictProductCache(product.getId());
        }
        order.setStatus(Order.Status.PLACED);
        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByUserId(order.getUserId());
        return saved;
    }

    /**
     * Cancels an order the user owns. A PLACED order restores the reserved stock;
     * an unpaid order is simply voided. Shipped/delivered/already-cancelled orders
     * cannot be cancelled.
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = getOwnedOrder(orderId, userId);
        switch (order.getStatus()) {
            case PLACED -> {
                for (OrderItem item : order.getItems()) {
                    productRepository.findById(item.getProductId()).ifPresent(product -> {
                        product.setStock(product.getStock() + item.getQuantity());
                        productRepository.save(product);
                        productService.evictProductCache(product.getId());
                    });
                }
                order.setStatus(Order.Status.CANCELLED);
            }
            case PENDING_PAYMENT, PAYMENT_FAILED -> order.setStatus(Order.Status.CANCELLED);
            default -> throw ApiException.badRequest(
                    "Order cannot be cancelled (status: " + order.getStatus() + ")");
        }
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public void markPaymentFailed(Long orderId) {
        orderRepository.findById(orderId).ifPresent(o -> {
            if (o.getStatus() == Order.Status.PENDING_PAYMENT) {
                o.setStatus(Order.Status.PAYMENT_FAILED);
                orderRepository.save(o);
            }
        });
    }

    public Order getOwnedOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw ApiException.notFound("Order not found");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProductId(), i.getProductName(), i.getPrice(), i.getQuantity()))
                .toList();
        return new OrderResponse(order.getId(), order.getTotal(),
                order.getStatus().name(), order.getCreatedAt(), items);
    }
}
