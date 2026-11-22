
package com.example.demo.modules.marketing.controller;

import com.example.demo.modules.marketing.dto.CreateMarketingStrategyRequest;
import com.example.demo.modules.marketing.dto.MarketingStrategyResponse;

import com.example.demo.modules.marketing.service.MarketingStrategyService;

import com.example.demo.modules.marketing.service.MarketingStrategyService.MarketingForbiddenException;
import com.example.demo.modules.marketing.service.MarketingStrategyService.MarketingDiscountNotFoundException;
import com.example.demo.modules.marketing.service.MarketingStrategyService.MarketingDiscountConflictException;

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
@RequestMapping("/api/marketing-strategies")
@Profile("local-demo")
public class MarketingStrategyController {

    private final MarketingStrategyService service;
    private final int demoManagerId;

    public MarketingStrategyController(
            MarketingStrategyService service,
            @Value("${RR8_DEMO_MANAGER_ID}") int demoManagerId) {

        this.service = service;
        this.demoManagerId = demoManagerId;
    }

    // US-11: Tao chien luoc marketing
    @PostMapping
    public ResponseEntity<MarketingStrategyResponse> createStrategy(
            @Valid @RequestBody CreateMarketingStrategyRequest request) {

        MarketingStrategyResponse result = service.createStrategy(
                request,
                demoManagerId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    // 400: Du lieu nghiep vu khong hop le
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest()
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

        return ResponseEntity.badRequest()
                .body(Map.of("message", message));
    }

    // 400: JSON sai dinh dang
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(
            HttpMessageNotReadableException ex) {

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "message",
                        "JSON khong hop le hoac sai dinh dang ngay"));
    }

    // 403: Khong phai Manager
    @ExceptionHandler(MarketingForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(
            MarketingForbiddenException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", ex.getMessage()));
    }

    // 404: Ma giam gia khong ton tai
    @ExceptionHandler(MarketingDiscountNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            MarketingDiscountNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    // 409: Ma giam gia khong du dieu kien
    @ExceptionHandler(MarketingDiscountConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            MarketingDiscountConflictException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    // 500: Trang thai du lieu bat thuong
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInternalError(
            IllegalStateException ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message",
                        "Khong the hoan tat viec tao chien luoc"));
    }
}
