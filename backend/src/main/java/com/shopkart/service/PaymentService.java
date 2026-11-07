package com.shopkart.service;

import com.shopkart.dto.OrderDtos.OrderResponse;
import com.shopkart.dto.PaymentDtos.*;
import com.shopkart.exception.ApiException;
import com.shopkart.model.Order;
import com.shopkart.model.Payment;
import com.shopkart.payment.MockPaymentGateway;
import com.shopkart.payment.MockPaymentGateway.ChargeResult;
import com.shopkart.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class PaymentService {

    private static final Duration PAY_LOCK_TTL = Duration.ofSeconds(15);
    private static final List<String> SUPPORTED_METHODS = List.of("CARD", "UPI", "NETBANKING");

    private final MockPaymentGateway gateway;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final RedisLockService lockService;

    public PaymentService(MockPaymentGateway gateway,
                          PaymentRepository paymentRepository,
                          OrderService orderService,
                          RedisLockService lockService) {
        this.gateway = gateway;
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.lockService = lockService;
    }

    /** Creates a gateway "order" (payment intent) for a pending order. */
    public CheckoutResponse createIntent(Order order) {
        String gatewayOrderId = gateway.createOrder(order.getTotal());
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .gatewayOrderId(gatewayOrderId)
                .amount(order.getTotal())
                .status(Payment.Status.CREATED)
                .createdAt(Instant.now())
                .build();
        paymentRepository.save(payment);
        return new CheckoutResponse(order.getId(), gatewayOrderId, order.getTotal(), "INR", SUPPORTED_METHODS);
    }

    /**
     * Charges the payment instrument via the mock gateway, verifies the returned
     * signature, and on success confirms the order (stock deduction + cart clear).
     * A Redis lock per order prevents double-charging on rapid retries.
     */
    public PayResponse pay(Long userId, PayRequest req) {
        Order order = orderService.getOwnedOrder(req.orderId(), userId);
        if (order.getStatus() != Order.Status.PENDING_PAYMENT) {
            throw ApiException.conflict("Order is not awaiting payment (status: " + order.getStatus() + ")");
        }

        String lockKey = "lock:pay:" + req.orderId();
        String token = lockService.tryAcquire(lockKey, PAY_LOCK_TTL);
        if (token == null) {
            throw ApiException.conflict("A payment is already being processed for this order.");
        }
        try {
            Payment payment = paymentRepository.findByOrderId(req.orderId())
                    .orElseThrow(() -> ApiException.badRequest("No payment intent for this order"));

            ChargeResult result = gateway.charge(payment.getGatewayOrderId(), req.method(), req.instrument());
            payment.setMethod(req.method());

            if (!result.success()) {
                // Keep the order PENDING_PAYMENT so the user can retry with a
                // valid instrument (mirrors real gateways allowing re-attempts).
                payment.setStatus(Payment.Status.FAILED);
                payment.setFailureReason(result.failureReason());
                paymentRepository.save(payment);
                return new PayResponse("FAILED", null, result.failureReason(), null);
            }

            // Server-side signature verification, exactly like a real gateway callback.
            if (!gateway.verify(payment.getGatewayOrderId(), result.paymentId(), result.signature())) {
                payment.setStatus(Payment.Status.FAILED);
                payment.setFailureReason("Signature verification failed");
                paymentRepository.save(payment);
                orderService.markPaymentFailed(req.orderId());
                throw ApiException.badRequest("Payment signature verification failed");
            }

            payment.setStatus(Payment.Status.SUCCESS);
            payment.setGatewayPaymentId(result.paymentId());
            paymentRepository.save(payment);

            Order confirmed = orderService.confirmOrder(req.orderId());
            OrderResponse orderResponse = orderService.toResponse(confirmed);
            return new PayResponse("SUCCESS", result.paymentId(), "Payment successful", orderResponse);
        } finally {
            lockService.release(lockKey, token);
        }
    }
}
