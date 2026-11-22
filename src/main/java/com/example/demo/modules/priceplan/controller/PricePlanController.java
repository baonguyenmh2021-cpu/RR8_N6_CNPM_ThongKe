
package com.example.demo.modules.priceplan.controller;

import com.example.demo.modules.priceplan.dto.CreatePricePlanRequest;
import com.example.demo.modules.priceplan.dto.PricePlanResponse;
import com.example.demo.modules.priceplan.service.PricePlanService;

import com.example.demo.modules.priceplan.service.PricePlanService.PricePlanForbiddenException;
import com.example.demo.modules.priceplan.service.PricePlanService.PricePlanNotFoundException;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

@RestController
@RequestMapping("/api/price-plans")
@Profile("local-demo")
public class PricePlanController {

    private final PricePlanService service;
    private final int demoManagerId;

    public PricePlanController(
            PricePlanService service,
            @Value("${RR8_DEMO_MANAGER_ID}") int demoManagerId) {

        this.service = service;
        this.demoManagerId = demoManagerId;
    }

    // US-09: Tao ke hoach dieu chinh gia
    @PostMapping
    public ResponseEntity<PricePlanResponse> createPricePlan(
            @Valid @RequestBody CreatePricePlanRequest request) {

        PricePlanResponse result = service.createPricePlan(
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
                .body(Map.of("message", ex.getMessage()));
    }

    // 400: Du lieu vi pham Bean Validation
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

    // 403: ID nguoi lap khong co vai tro Manager
    @ExceptionHandler(PricePlanForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(
            PricePlanForbiddenException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", ex.getMessage()));
    }

    // 404: Khong tim thay loai phong
    @ExceptionHandler(PricePlanNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            PricePlanNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    // 409: Xung dot nghiep vu
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            IllegalStateException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }
}
