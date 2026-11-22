
package com.example.demo.modules.marketing.controller;

import com.example.demo.modules.marketing.dto.MarketingStrategySummaryResponse;
import com.example.demo.modules.marketing.service.MarketingStrategyQueryService;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketing-strategies")
@Profile("local-demo")
public class MarketingStrategyQueryController {

    private final MarketingStrategyQueryService service;

    public MarketingStrategyQueryController(
            MarketingStrategyQueryService service) {

        this.service = service;
    }

    // US-13: Xem tat ca hoac loc theo trang thai
    @GetMapping
    public ResponseEntity<List<MarketingStrategySummaryResponse>> getStrategies(
            @RequestParam(required = false) String status) {

        List<MarketingStrategySummaryResponse> results = service.getStrategies(status);

        return ResponseEntity.ok(results);
    }

    // 400: Trang thai loc khong hop le
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "message",
                        ex.getMessage()));
    }
}
