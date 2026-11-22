
package com.example.demo.modules.revenue.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;



import org.springframework.stereotype.Service;

import com.example.demo.modules.revenue.dto.CategoryRevenueResponse;
import com.example.demo.modules.revenue.dto.CategoryRevenueResponse.CategoryRevenueItem;
import com.example.demo.modules.revenue.dto.MonthlyRevenueResponse;
import com.example.demo.modules.revenue.repository.RevenueRepository;
import com.example.demo.modules.revenue.repository.RevenueRepository.BookingSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.demo.modules.revenue.dto.YearlyRevenueResponse;
import com.example.demo.modules.revenue.dto.YearlyRevenueResponse.MonthlyRevenueItem;

import java.util.Optional;
import com.example.demo.modules.revenue.dto.ClosedMonthlyReportResponse;

@Service
public class RevenueService {

    private final RevenueRepository revenueRepository;

    public RevenueService(RevenueRepository revenueRepository) {
        this.revenueRepository = revenueRepository;
    }

    // US-01: Xem doanh thu theo thang
    public MonthlyRevenueResponse getMonthlyRevenue(int year, int month) {

        // 1. Kiem tra thang va nam
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Thang phai tu 1 den 12");
        }

        if (year < 1753 || year > 9998) {
            throw new IllegalArgumentException("Nam khong hop le");
        }

        // 2. Xac dinh khoang thoi gian
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.plusMonths(1);

        // 3. Lay doanh thu thang hien tai
        BigDecimal revenue = revenueRepository.getRevenue(from, to);

        // 4. Lay so booking va tong chiet khau
        BookingSummary summary = revenueRepository.getBookingSummary(from, to);

        // 5. Lay so booking tao trong thang hien da huy
        long cancelledBookings = revenueRepository.getCancelledBookings(from, to);

        // 6. Lay doanh thu thang truoc
        LocalDate previousFrom = from.minusMonths(1);

        BigDecimal previousRevenue = revenueRepository.getRevenue(
                previousFrom, from);

        // 7. Tinh phan tram tang truong
        BigDecimal growthPercent = null;

        if (previousRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growthPercent = revenue.subtract(previousRevenue)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousRevenue, 1, RoundingMode.HALF_UP);
        }

        // 8. Tra ve ket qua
        return new MonthlyRevenueResponse(
                year,
                month,
                revenue,
                summary.bookingCount(),
                summary.totalDiscount(),
                cancelledBookings,
                previousRevenue,
                growthPercent);
    }


    // US-02: Xem doanh thu 12 thang trong nam
    public YearlyRevenueResponse getYearlyRevenue(int year) {

        // 1. Kiem tra nam
        if (year < 1753 || year > 9998) {
            throw new IllegalArgumentException("Nam khong hop le");
        }

        // 2. Lay du lieu doanh thu tu Repository
        Map<Integer, BigDecimal> revenueMap = revenueRepository.getMonthlyRevenuesByYear(year);

        List<MonthlyRevenueItem> months = new ArrayList<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal maxRevenue = BigDecimal.ZERO;

        // 3. Duyet du 12 thang
        for (int month = 1; month <= 12; month++) {

            // Thang khong co giao dich thi doanh thu = 0
            BigDecimal revenue = revenueMap.getOrDefault(
                    month, BigDecimal.ZERO);

            months.add(new MonthlyRevenueItem(month, revenue));

            // Cong doanh thu ca nam
            totalRevenue = totalRevenue.add(revenue);

            // Tim doanh thu cao nhat
            if (revenue.compareTo(maxRevenue) > 0) {
                maxRevenue = revenue;
            }
        }

        // 4. Tim cac thang co doanh thu cao nhat
        List<Integer> highestMonths = new ArrayList<>();

        for (MonthlyRevenueItem item : months) {
            if (item.revenue().compareTo(maxRevenue) == 0) {
                highestMonths.add(item.month());
            }
        }

        // 5. Tra ve ket qua
        return new YearlyRevenueResponse(
                year,
                totalRevenue,
                months,
                highestMonths);
    }

    // US-03: Thong ke doanh thu theo loai phong
    public CategoryRevenueResponse getCategoryRevenue(
            int year, Integer month) {

        // 1. Kiem tra nam hop le
        if (year < 1753 || year > 9998) {
            throw new IllegalArgumentException(
                    "Nam khong hop le");
        }

        // 2. Kiem tra thang neu co
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException(
                    "Thang phai tu 1 den 12");
        }

        LocalDate from;
        LocalDate to;

        // 3. Xac dinh ky thong ke
        if (month == null) {
            // Thong ke ca nam
            from = LocalDate.of(year, 1, 1);
            to = from.plusYears(1);
        } else {
            // Thong ke theo thang
            from = LocalDate.of(year, month, 1);
            to = from.plusMonths(1);
        }

        // 4. Lay doanh thu tung loai phong
        List<CategoryRevenueItem> categories =
                revenueRepository.getCategoryRevenues(from, to);

        // 5. Tinh tong doanh thu
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (CategoryRevenueItem item : categories) {
            totalRevenue = totalRevenue.add(item.revenue());
        }

        // 6. Tra ket qua
        return new CategoryRevenueResponse(
                year,
                month,
                totalRevenue,
                categories
        );
    }


    // US-04: Xem bao cao doanh thu thang da chot
    public Optional<ClosedMonthlyReportResponse> getClosedMonthlyReport(
            int year, int month) {

        // 1. Kiem tra nam hop le
        if (year < 1753 || year > 9998) {
            throw new IllegalArgumentException(
                    "Nam khong hop le");
        }

        // 2. Kiem tra thang hop le
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException(
                    "Thang phai tu 1 den 12");
        }

        // 3. Doc bao cao da chot tu database
        return revenueRepository.getClosedMonthlyReport(year, month);
    }


    // US-04: Chot bao cao doanh thu thang
    // adminId phai duoc cung cap boi backend da xac thuc
    public ClosedMonthlyReportResponse closeMonthlyReport(
            int year, int month, int adminId) {

        // 1. Kiem tra nam
        if (year < 1753 || year > 9998) {
            throw new IllegalArgumentException(
                    "Nam khong hop le");
        }

        // 2. Kiem tra thang
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException(
                    "Thang phai tu 1 den 12");
        }

        // 3. Kiem tra ID nguoi thuc hien
        if (adminId <= 0) {
            throw new IllegalArgumentException(
                    "Nguoi thuc hien khong hop le");
        }

        // 4. Goi Repository de chot bao cao
        // Stored procedure se kiem tra vai tro Admin
        // va tu choi neu bao cao da duoc chot
        return revenueRepository.closeMonthlyReport(
                year, month, adminId);
    }

}
