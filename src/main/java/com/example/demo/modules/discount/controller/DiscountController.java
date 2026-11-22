
package com.example.demo.modules.discount.controller;

import com.example.demo.modules.discount.dto.CreateDiscountRequest;
import com.example.demo.modules.discount.dto.DiscountResponse;
import com.example.demo.modules.discount.service.DiscountService;
import com.example.demo.modules.discount.service.DiscountService.DuplicateDiscountCodeException;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/discounts")
@Profile("local-demo")
public class DiscountController {

    private final DiscountService discountService;
    private final int demoManagerId;

    public DiscountController(
            DiscountService discountService,
            @Value("${RR8_DEMO_MANAGER_ID}") int demoManagerId) {

        this.discountService = discountService;
        this.demoManagerId = demoManagerId;
    }

    // US-05: Tao ma giam gia
    @PostMapping
    public ResponseEntity<DiscountResponse> createDiscount(
            @Valid @RequestBody CreateDiscountRequest request) {

        DiscountResponse result = discountService.createDiscount(
                request, demoManagerId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    // Ma giam gia bi trung
    @ExceptionHandler(DuplicateDiscountCodeException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(
            DuplicateDiscountCodeException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    // Loi nghiep vu
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidData(
            IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", ex.getMessage()));
    }

    // Loi kiem tra du lieu tu JSON
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Du lieu khong hop le");

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", message));
    }
}
