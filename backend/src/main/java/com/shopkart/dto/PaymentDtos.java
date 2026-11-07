package com.shopkart.dto;

import com.shopkart.dto.OrderDtos.OrderResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class PaymentDtos {

    /** Returned by checkout — the "payment intent" the UI needs to show the pay page. */
    public record CheckoutResponse(
            Long orderId,
            String gatewayOrderId,
            BigDecimal amount,
            String currency,
            List<String> supportedMethods
    ) {}

    public record PayRequest(
            @NotNull Long orderId,
            @NotBlank String method,      // CARD | UPI | NETBANKING
            @NotBlank String instrument   // card number | upi id | bank code
    ) {}

    public record PayResponse(
            String status,        // SUCCESS | FAILED
            String paymentId,
            String message,
            OrderResponse order
    ) {}
}
