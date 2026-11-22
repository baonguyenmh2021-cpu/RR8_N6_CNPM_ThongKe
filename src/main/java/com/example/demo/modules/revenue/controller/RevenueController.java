
package com.example.demo.modules.revenue.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.modules.revenue.dto.MonthlyRevenueResponse;
import com.example.demo.modules.revenue.service.RevenueService;

import com.example.demo.modules.revenue.dto.YearlyRevenueResponse;

import com.example.demo.modules.revenue.dto.CategoryRevenueResponse;

import org.springframework.http.ResponseEntity;
import com.example.demo.modules.revenue.dto.ClosedMonthlyReportResponse;

@RestController
@RequestMapping("/api/revenue")
public class RevenueController {

    private final RevenueService revenueService;

    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    // US-01: Xem doanh thu theo thang
    @GetMapping("/monthly")
    public MonthlyRevenueResponse getMonthlyRevenue(
            @RequestParam int year,
            @RequestParam int month) {

        return revenueService.getMonthlyRevenue(year, month);
    }

    // Xu ly loi du lieu dau vao
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidInput(
            IllegalArgumentException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }


    // US-02: Xem doanh thu 12 thang trong nam
    @GetMapping("/yearly")
    public YearlyRevenueResponse getYearlyRevenue(
            @RequestParam int year) {

        return revenueService.getYearlyRevenue(year);
    }



    // US-03: Thong ke doanh thu theo loai phong
    @GetMapping("/categories")
    public CategoryRevenueResponse getCategoryRevenue(
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {

        return revenueService.getCategoryRevenue(year, month);
    }


    // US-04: Xem bao cao doanh thu thang da chot
    @GetMapping("/closed")
    public ResponseEntity<ClosedMonthlyReportResponse> getClosedMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {

        return revenueService.getClosedMonthlyReport(year, month)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
