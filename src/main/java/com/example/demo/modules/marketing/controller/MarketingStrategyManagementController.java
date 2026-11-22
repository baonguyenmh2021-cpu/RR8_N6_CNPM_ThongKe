
package com.example.demo.modules.marketing.controller;

import com.example.demo.modules.marketing.dto.ChangeMarketingStrategyStatusRequest;
import com.example.demo.modules.marketing.dto.MarketingStrategyResponse;
import com.example.demo.modules.marketing.dto.UpdateMarketingStrategyRequest;

import com.example.demo.modules.marketing.service.MarketingStrategyManagementService;
import com.example.demo.modules.marketing.service.MarketingStrategyManagementService.ManagementForbiddenException;
import com.example.demo.modules.marketing.service.MarketingStrategyManagementService.ManagementStrategyNotFoundException;
import com.example.demo.modules.marketing.service.MarketingStrategyManagementService.ManagementDiscountNotFoundException;
import com.example.demo.modules.marketing.service.MarketingStrategyManagementService.ManagementConflictException;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestController
@RequestMapping("/api/marketing-strategies")
@Profile("local-demo")
public class MarketingStrategyManagementController {

    private final MarketingStrategyManagementService service;

    // ID Manager chi dung cho moi truong demo
    @Value("${RR8_DEMO_MANAGER_ID:0}")
    private int demoManagerId;

    public MarketingStrategyManagementController(
            MarketingStrategyManagementService service) {

        this.service = service;
    }

    // =========================================
    // US-14A: CAP NHAT NOI DUNG CHIEN LUOC
    // =========================================

    @PutMapping("/{strategyId}")
    public ResponseEntity<MarketingStrategyResponse> updateStrategy(
            @PathVariable int strategyId,
            @Valid @RequestBody UpdateMarketingStrategyRequest request) {

        MarketingStrategyResponse result = service.updateDetails(
                strategyId,
                request,
                demoManagerId);

        return ResponseEntity.ok(result);
    }

    // =========================================
    // US-14B: CHUYEN TRANG THAI CHIEN LUOC
    // =========================================

    @PatchMapping("/{strategyId}/status")
    public ResponseEntity<MarketingStrategyResponse> changeStatus(
            @PathVariable int strategyId,
            @Valid @RequestBody ChangeMarketingStrategyStatusRequest request) {

        MarketingStrategyResponse result = service.changeStrategyStatus(
                strategyId,
                request,
                demoManagerId);

        return ResponseEntity.ok(result);
    }

    // =========================================
    // XU LY LOI HTTP 400
    // =========================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // Du lieu vi pham @NotNull, @NotBlank, @Pattern...
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .filter(text -> text != null)
                .orElse("Du lieu cap nhat khong hop le");

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", message));
    }

    // JSON rong, sai dinh dang hoac sai kieu du lieu
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(
            HttpMessageNotReadableException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        "JSON khong hop le hoac sai dinh dang du lieu"));
    }

    // ID tren URL khong phai so nguyen
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPath(
            MethodArgumentTypeMismatchException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        "ID chien luoc phai la so nguyen"));
    }

    // =========================================
    // XU LY LOI HTTP 403
    // =========================================

    @ExceptionHandler(ManagementForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(
            ManagementForbiddenException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // =========================================
    // XU LY LOI HTTP 404
    // =========================================

    @ExceptionHandler({
            ManagementStrategyNotFoundException.class,
            ManagementDiscountNotFoundException.class
    })
    public ResponseEntity<Map<String, String>> handleNotFound(
            RuntimeException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // =========================================
    // XU LY LOI HTTP 409
    // =========================================

    @ExceptionHandler(ManagementConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            ManagementConflictException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // =========================================
    // XU LY LOI HTTP 500
    // =========================================

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInternalError(
            IllegalStateException ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message",
                        "Khong the hoan tat cap nhat chien luoc"));
    }
}
