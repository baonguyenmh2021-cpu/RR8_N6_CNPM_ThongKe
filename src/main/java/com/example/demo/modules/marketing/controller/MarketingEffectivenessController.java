
package com.example.demo.modules.marketing.controller;

import com.example.demo.modules.marketing.dto.MarketingEffectivenessResponse;

import com.example.demo.modules.marketing.service.MarketingEffectivenessService;
import com.example.demo.modules.marketing.service.MarketingEffectivenessService.MarketingStrategyNotFoundException;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/marketing-strategies")
@Profile("local-demo")
public class MarketingEffectivenessController {

    private final MarketingEffectivenessService service;

    public MarketingEffectivenessController(
            MarketingEffectivenessService service) {

        this.service = service;
    }

    // US-12: Danh gia hieu qua mot chien luoc
    @GetMapping("/{strategyId}/effectiveness")
    public ResponseEntity<MarketingEffectivenessResponse> getEffectiveness(
            @PathVariable int strategyId) {

        MarketingEffectivenessResponse result = service.getEffectiveness(strategyId);

        return ResponseEntity.ok(result);
    }

    // 400: ID chien luoc khong hop le
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // 404: Khong tim thay chien luoc
    @ExceptionHandler(MarketingStrategyNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            MarketingStrategyNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }

    // 500: Du lieu thong ke khong nhat quan
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidData(
            IllegalStateException ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message",
                        "Du lieu danh gia chien luoc khong nhat quan"));
    }
}
