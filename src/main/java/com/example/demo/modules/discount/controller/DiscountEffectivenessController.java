
package com.example.demo.modules.discount.controller;

import com.example.demo.modules.discount.dto.DiscountEffectivenessResponse;
import com.example.demo.modules.discount.service.DiscountEffectivenessService;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discounts")
@Profile("local-demo")
public class DiscountEffectivenessController {

    private final DiscountEffectivenessService service;

    public DiscountEffectivenessController(
            DiscountEffectivenessService service) {
        this.service = service;
    }

    // US-06: Thong ke hieu qua ma giam gia
    @GetMapping("/effectiveness")
    public ResponseEntity<List<DiscountEffectivenessResponse>> getEffectiveness(

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<DiscountEffectivenessResponse> result = service.getEffectiveness(from, to);

        return ResponseEntity.ok(result);
    }

    // Xu ly loi khoang thoi gian khong hop le
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidDate(
            IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", ex.getMessage()));
    }
}
