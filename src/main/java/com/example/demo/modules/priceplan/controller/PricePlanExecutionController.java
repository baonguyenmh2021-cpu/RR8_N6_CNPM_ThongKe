
package com.example.demo.modules.priceplan.controller;

import com.example.demo.modules.priceplan.repository.PricePlanExecutionRepository.PricePlanSnapshot;

import com.example.demo.modules.priceplan.service.PricePlanExecutionService;

import com.example.demo.modules.priceplan.service.PricePlanExecutionService.PricePlanExecutionForbiddenException;
import com.example.demo.modules.priceplan.service.PricePlanExecutionService.PricePlanExecutionNotFoundException;
import com.example.demo.modules.priceplan.service.PricePlanExecutionService.PricePlanExecutionConflictException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestController
@RequestMapping("/api/price-plans")
@Profile("local-demo")
public class PricePlanExecutionController {

    private final PricePlanExecutionService service;

    // Chi dung cho moi truong demo noi bo
    @Value("${RR8_DEMO_ADMIN_ID:0}")
    private int demoAdminId;

    public PricePlanExecutionController(
            PricePlanExecutionService service) {

        this.service = service;
    }

    // =========================================
    // US-15A: AP DUNG KE HOACH GIA
    // Approved -> Applied
    // =========================================

    @PostMapping("/{planId}/apply")
    public ResponseEntity<PricePlanSnapshot> applyPlan(
            @PathVariable int planId) {

        PricePlanSnapshot result = service.applyPlan(
                planId,
                demoAdminId);

        return ResponseEntity.ok(result);
    }

    // =========================================
    // US-15B: KET THUC KE HOACH GIA
    // Applied -> Ended
    // =========================================

    @PostMapping("/{planId}/end")
    public ResponseEntity<PricePlanSnapshot> endPlan(
            @PathVariable int planId) {

        PricePlanSnapshot result = service.endPlan(
                planId,
                demoAdminId);

        return ResponseEntity.ok(result);
    }

    // =========================================
    // HTTP 400 - BAD REQUEST
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

    // ID URL khong phai so nguyen
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleInvalidId(
            MethodArgumentTypeMismatchException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        "ID ke hoach phai la so nguyen"));
    }

    // =========================================
    // HTTP 403 - FORBIDDEN
    // =========================================

    @ExceptionHandler(PricePlanExecutionForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(
            PricePlanExecutionForbiddenException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // =========================================
    // HTTP 404 - NOT FOUND
    // =========================================

    @ExceptionHandler(PricePlanExecutionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            PricePlanExecutionNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // =========================================
    // HTTP 409 - CONFLICT
    // =========================================

    @ExceptionHandler(PricePlanExecutionConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            PricePlanExecutionConflictException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // =========================================
    // HTTP 500 - INTERNAL SERVER ERROR
    // =========================================

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInternalError(
            IllegalStateException ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message",
                        "Du lieu ke hoach gia khong nhat quan"));
    }
}
