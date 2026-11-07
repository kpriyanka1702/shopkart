package com.shopkart.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** Gateway-side order/intent id (like Razorpay order_xxx). */
    @Column(nullable = false)
    private String gatewayOrderId;

    /** Gateway-side payment id, populated after a charge attempt (pay_xxx). */
    private String gatewayPaymentId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    public enum Status {
        CREATED, SUCCESS, FAILED
    }
}
