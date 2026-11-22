
package com.example.demo.modules.occupancy.controller;

import com.example.demo.modules.occupancy.dto.OccupancyCategoryResponse;
import com.example.demo.modules.occupancy.service.OccupancyService;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/occupancy")
@Profile("local-demo")
public class OccupancyController {

    private final OccupancyService service;

    public OccupancyController(OccupancyService service) {
        this.service = service;
    }

    // US-08: Thong ke ty le lap day theo loai phong
    @GetMapping("/categories")
    public ResponseEntity<List<OccupancyCategoryResponse>> getOccupancy(

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<OccupancyCategoryResponse> result = service.getOccupancy(from, to);

        return ResponseEntity.ok(result);
    }

    // Xu ly loi ngay khong hop le
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidDate(
            IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", ex.getMessage()));
    }

    // Xu ly du lieu thong ke bat thuong
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidData(
            IllegalStateException ex) {

        return ResponseEntity
                .status(500)
                .body(Map.of(
                        "message",
                        "Du lieu thong ke lap day khong nhat quan"));
    }
}
