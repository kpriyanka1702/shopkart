package com.shopkart.controller;

import com.shopkart.dto.PaymentDtos.*;
import com.shopkart.service.CurrentUserService;
import com.shopkart.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService paymentService, CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/pay")
    public ResponseEntity<PayResponse> pay(@Valid @RequestBody PayRequest request) {
        Long userId = currentUserService.require().getId();
        return ResponseEntity.ok(paymentService.pay(userId, request));
    }
}
