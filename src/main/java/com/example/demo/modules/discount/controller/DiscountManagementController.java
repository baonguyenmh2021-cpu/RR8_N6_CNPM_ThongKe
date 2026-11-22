
package com.example.demo.modules.discount.controller;

import com.example.demo.modules.discount.dto.DiscountEligibilityResponse;
import com.example.demo.modules.discount.dto.ExtendDiscountRequest;
import com.example.demo.modules.discount.repository.DiscountManagementRepository.DiscountState;
import com.example.demo.modules.discount.service.DiscountManagementService;
import com.example.demo.modules.discount.service.DiscountManagementService.DiscountNotFoundException;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/discounts")
@Profile("local-demo")
public class DiscountManagementController {

    private final DiscountManagementService service;

    public DiscountManagementController(
            DiscountManagementService service) {

        this.service = service;
    }

    // US-07: Kiem tra ma co ap dung duoc khong
    @GetMapping("/{discountId}/eligibility")
    public ResponseEntity<DiscountEligibilityResponse> checkEligibility(

            @PathVariable int discountId,

            @RequestParam BigDecimal orderAmount) {

        DiscountEligibilityResponse result = service.checkEligibility(
                discountId, orderAmount);

        return ResponseEntity.ok(result);
    }

    // US-07: Vo hieu hoa ma giam gia
    @PatchMapping("/{discountId}/disable")
    public ResponseEntity<DiscountState> disableDiscount(
            @PathVariable int discountId) {

        DiscountState result = service.disableDiscount(discountId);

        return ResponseEntity.ok(result);
    }

    // US-07: Gia han ma giam gia
    @PatchMapping("/{discountId}/extend")
    public ResponseEntity<DiscountState> extendDiscount(

            @PathVariable int discountId,

            @Valid @RequestBody ExtendDiscountRequest request) {

        DiscountState result = service.extendDiscount(
                discountId,
                request.newValidTo());

        return ResponseEntity.ok(result);
    }

    // Khong tim thay ma giam gia
    @ExceptionHandler(DiscountNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            DiscountNotFoundException ex) {

        return ResponseEntity.status(404)
                .body(Map.of("message", ex.getMessage()));
    }

    // Du lieu khong hop le
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest()
                .body(Map.of("message", ex.getMessage()));
    }

    // Trang thai khong cho phep cap nhat
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            IllegalStateException ex) {

        return ResponseEntity.status(409)
                .body(Map.of("message", ex.getMessage()));
    }

    // Thieu ngay het han moi
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Du lieu khong hop le");

        return ResponseEntity.badRequest()
                .body(Map.of("message", message));
    }
}
