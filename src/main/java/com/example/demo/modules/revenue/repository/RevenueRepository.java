
package com.example.demo.modules.revenue.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.Map;
import java.util.LinkedHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

import com.example.demo.modules.revenue.dto.CategoryRevenueResponse.CategoryRevenueItem;

import java.util.List;
import java.util.Optional;

import com.example.demo.modules.revenue.dto.ClosedMonthlyReportResponse;

import java.sql.CallableStatement;
import org.springframework.jdbc.core.ConnectionCallback;

@Repository
public class RevenueRepository {

    private final JdbcTemplate jdbcTemplate;

    public RevenueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 1. Tinh doanh thu trong ky
    public BigDecimal getRevenue(LocalDate from, LocalDate to) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM payments
                WHERE status = 'Paid'
                  AND paid_at >= ?
                  AND paid_at < ?
                """;

        return jdbcTemplate.queryForObject(
                sql, BigDecimal.class, from, to);
    }

    // 2. Dem booking va tinh tong chiet khau
    public BookingSummary getBookingSummary(
            LocalDate from, LocalDate to) {

        String sql = """
                SELECT
                    COUNT(*) AS booking_count,
                    COALESCE(SUM(b.discount_amount), 0)
                        AS total_discount
                FROM bookings b
                JOIN (
                    SELECT booking_id,
                           MIN(paid_at) AS first_paid_at
                    FROM payments
                    WHERE status = 'Paid'
                    GROUP BY booking_id
                ) fp ON b.booking_id = fp.booking_id
                WHERE fp.first_paid_at >= ?
                  AND fp.first_paid_at < ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> new BookingSummary(
                        rs.getLong("booking_count"),
                        rs.getBigDecimal("total_discount")),
                from, to);
    }

    // 3. Dem booking tao trong ky hien da huy
    public long getCancelledBookings(
            LocalDate from, LocalDate to) {

        String sql = """
                SELECT COUNT(*)
                FROM bookings
                WHERE status = 'Cancelled'
                  AND created_at >= ?
                  AND created_at < ?
                """;

        Long count = jdbcTemplate.queryForObject(
                sql, Long.class, from, to);

        return count == null ? 0 : count;
    }

    public record BookingSummary(
            long bookingCount,
            BigDecimal totalDiscount) {
    }


    // US-02: Lay doanh thu 12 thang trong nam
    public Map<Integer, BigDecimal> getMonthlyRevenuesByYear(int year) {

            LocalDate from = LocalDate.of(year, 1, 1);
            LocalDate to = from.plusYears(1);

            String sql = """
                            SELECT
                                MONTH(paid_at) AS revenue_month,
                                SUM(amount) AS revenue
                            FROM dbo.payments
                            WHERE status = 'Paid'
                              AND paid_at >= ?
                              AND paid_at < ?
                            GROUP BY MONTH(paid_at)
                            ORDER BY revenue_month
                            """;

            Map<Integer, BigDecimal> result = new LinkedHashMap<>();

            jdbcTemplate.query(sql, rs -> {
                    int month = rs.getInt("revenue_month");
                    BigDecimal revenue = rs.getBigDecimal("revenue");

                    result.put(month, revenue);
            }, from, to);

            return result;
    }


    // US-03: Thong ke doanh thu theo loai phong
    public List<CategoryRevenueItem> getCategoryRevenues(
                    LocalDate from, LocalDate to) {

            String sql = """
                            WITH revenue_by_category AS (
                                SELECT
                                    r.category_id,
                                    SUM(p.amount) AS revenue
                                FROM dbo.payments p
                                JOIN dbo.bookings b
                                    ON p.booking_id = b.booking_id
                                JOIN dbo.rooms r
                                    ON b.room_id = r.room_id
                                WHERE p.status = 'Paid'
                                  AND p.paid_at >= ?
                                  AND p.paid_at < ?
                                GROUP BY r.category_id
                            ),

                            first_paid AS (
                                SELECT
                                    booking_id,
                                    MIN(paid_at) AS first_paid_at
                                FROM dbo.payments
                                WHERE status = 'Paid'
                                GROUP BY booking_id
                            ),

                            bookings_by_category AS (
                                SELECT
                                    r.category_id,
                                    COUNT(*) AS booking_count
                                FROM first_paid fp
                                JOIN dbo.bookings b
                                    ON fp.booking_id = b.booking_id
                                JOIN dbo.rooms r
                                    ON b.room_id = r.room_id
                                WHERE fp.first_paid_at >= ?
                                  AND fp.first_paid_at < ?
                                GROUP BY r.category_id
                            )

                            SELECT
                                c.category_id,
                                c.name AS category_name,
                                COALESCE(rc.revenue, 0) AS revenue,
                                COALESCE(bc.booking_count, 0) AS booking_count
                            FROM dbo.categories c
                            LEFT JOIN revenue_by_category rc
                                ON c.category_id = rc.category_id
                            LEFT JOIN bookings_by_category bc
                                ON c.category_id = bc.category_id
                            ORDER BY c.category_id
                            """;

            return jdbcTemplate.query(
                            sql,
                            (rs, rowNum) -> new CategoryRevenueItem(
                                            rs.getInt("category_id"),
                                            rs.getString("category_name"),
                                            rs.getBigDecimal("revenue"),
                                            rs.getLong("booking_count")),
                            from, to, from, to);
    }


    // US-04: Lay bao cao thang da chot
    public Optional<ClosedMonthlyReportResponse> getClosedMonthlyReport(
                    int year, int month) {

            String sql = """
                            SELECT
                                report_id,
                                period_year,
                                period_month,
                                total_revenue,
                                total_discount,
                                total_bookings,
                                cancelled_bookings,
                                generated_by,
                                generated_at
                            FROM dbo.revenue_reports
                            WHERE period_year = ?
                              AND period_month = ?
                            """;

            List<ClosedMonthlyReportResponse> reports = jdbcTemplate.query(
                            sql,
                            (rs, rowNum) -> new ClosedMonthlyReportResponse(
                                            rs.getInt("report_id"),
                                            rs.getInt("period_year"),
                                            rs.getInt("period_month"),
                                            rs.getBigDecimal("total_revenue"),
                                            rs.getBigDecimal("total_discount"),
                                            rs.getLong("total_bookings"),
                                            rs.getLong("cancelled_bookings"),
                                            rs.getInt("generated_by"),
                                            rs.getTimestamp("generated_at")
                                                            .toLocalDateTime()),
                            year, month);

            return reports.stream().findFirst();
    }


    // US-04: Chot bao cao doanh thu thang
    public ClosedMonthlyReportResponse closeMonthlyReport(
                    int year, int month, int adminId) {

            // Goi stored procedure trong SQL Server
            jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {

                    String sql = "{call dbo.sp_close_monthly_report(?, ?, ?)}";

                    try (CallableStatement statement = connection.prepareCall(sql)) {

                            statement.setInt(1, year);
                            statement.setInt(2, month);
                            statement.setInt(3, adminId);

                            statement.execute();
                    }

                    return null;
            });

            // Doc lai ban chot vua luu
            return getClosedMonthlyReport(year, month)
                            .orElseThrow(() -> new IllegalStateException(
                                            "Khong tim thay bao cao sau khi chot"));
    }

}
