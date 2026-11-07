package com.shopkart.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * A self-contained mock payment gateway that mimics a real provider
 * (e.g. Razorpay/Stripe test mode):
 *
 * <ul>
 *   <li>createOrder(amount) -> a gateway order id (like {@code order_xxx})</li>
 *   <li>charge(...) -> simulates success/failure based on well-known TEST instruments</li>
 *   <li>signature over "gatewayOrderId|paymentId" using HMAC-SHA256</li>
 *   <li>verify(...) -> constant-time signature check</li>
 * </ul>
 *
 * No real money or network calls are involved. The signing secret is loaded
 * from configuration (env), never hardcoded.
 */
@Component
public class MockPaymentGateway {

    /** Test instruments — mirror the idea of provider "test cards". */
    public static final String TEST_CARD_SUCCESS = "4111111111111111";
    public static final String TEST_CARD_FAILURE = "4000000000000002";
    public static final String TEST_UPI_SUCCESS = "success@upi";
    public static final String TEST_UPI_FAILURE = "failure@upi";

    private final byte[] secret;

    public MockPaymentGateway(@Value("${app.payment.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String createOrder(BigDecimal amount) {
        return "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public ChargeResult charge(String gatewayOrderId, String method, String instrument) {
        String normalized = instrument == null ? "" : instrument.replaceAll("\\s+", "");
        boolean success = isSuccessInstrument(method, normalized);

        if (!success) {
            return ChargeResult.failure(declineReason(method, normalized));
        }

        String paymentId = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String signature = sign(gatewayOrderId + "|" + paymentId);
        return ChargeResult.success(paymentId, signature);
    }

    public boolean verify(String gatewayOrderId, String paymentId, String signature) {
        if (gatewayOrderId == null || paymentId == null || signature == null) {
            return false;
        }
        String expected = sign(gatewayOrderId + "|" + paymentId);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isSuccessInstrument(String method, String instrument) {
        if ("CARD".equalsIgnoreCase(method)) {
            return TEST_CARD_SUCCESS.equals(instrument);
        }
        if ("UPI".equalsIgnoreCase(method)) {
            return TEST_UPI_SUCCESS.equalsIgnoreCase(instrument);
        }
        if ("NETBANKING".equalsIgnoreCase(method)) {
            // Any non-empty selection succeeds except an explicit failure flag.
            return !instrument.isEmpty() && !"FAIL".equalsIgnoreCase(instrument);
        }
        return false;
    }

    private String declineReason(String method, String instrument) {
        if (instrument.isEmpty()) {
            return "No payment instrument provided";
        }
        if (TEST_CARD_FAILURE.equals(instrument) || TEST_UPI_FAILURE.equalsIgnoreCase(instrument)) {
            return "Payment declined by bank (test failure instrument)";
        }
        return "Invalid test instrument. Use the provided TEST card/UPI to succeed.";
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign payment", e);
        }
    }

    public record ChargeResult(boolean success, String paymentId, String signature, String failureReason) {
        static ChargeResult success(String paymentId, String signature) {
            return new ChargeResult(true, paymentId, signature, null);
        }
        static ChargeResult failure(String reason) {
            return new ChargeResult(false, null, null, reason);
        }
    }
}
