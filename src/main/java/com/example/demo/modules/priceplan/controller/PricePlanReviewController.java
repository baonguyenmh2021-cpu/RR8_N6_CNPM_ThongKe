
package com.example.demo.modules.priceplan.controller;

import com.example.demo.modules.priceplan.dto.ReviewPricePlanRequest;
import com.example.demo.modules.priceplan.dto.ReviewPricePlanResponse;

import com.example.demo.modules.priceplan.service.PricePlanReviewService;
import com.example.demo.modules.priceplan.service.PricePlanReviewService.PricePlanReviewForbiddenException;
import com.example.demo.modules.priceplan.service.PricePlanReviewService.PricePlanReviewNotFoundException;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/price-plans")
@Profile("local-demo")
public class PricePlanReviewController {

    private final PricePlanReviewService service;
    private final int demoAdminId;

    public PricePlanReviewController(
            PricePlanReviewService service,
            @Value("${RR8_DEMO_ADMIN_ID}") int demoAdminId) {

        this.service = service;
        this.demoAdminId = demoAdminId;
    }

    // US-10: Admin duyet hoac tu choi ke hoach
    @PatchMapping("/{planId}/review")
    public ResponseEntity<ReviewPricePlanResponse> reviewPricePlan(

            @PathVariable int planId,

            @Valid @RequestBody ReviewPricePlanRequest request) {

        ReviewPricePlanResponse result = service.reviewPricePlan(
                planId,
                request,
                demoAdminId);

        return ResponseEntity.ok(result);
    }

    // 400: Du lieu nghiep vu khong hop le
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // 400: Loi Bean Validation
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
                .body(Map.of(
                        "message",
                        message));
    }

    // 400: JSON khong dung dinh dang
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(
            HttpMessageNotReadableException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        "JSON khong hop le"));
    }

    // 403: Khong duoc phep xet duyet
    @ExceptionHandler(PricePlanReviewForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(
            PricePlanReviewForbiddenException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // 404: Ke hoach khong ton tai
    @ExceptionHandler(PricePlanReviewNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            PricePlanReviewNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // 409: Xung dot nghiep vu
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            IllegalStateException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }
}
