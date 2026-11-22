
package com.example.demo.modules.marketing.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

import java.time.LocalDate;

@Repository
public class MarketingEffectivenessRepository {

    private final JdbcTemplate jdbcTemplate;

    public MarketingEffectivenessRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    // Du lieu thong ke tho cho Service xu ly
    public record MarketingMetrics(
            int strategyId,
            String title,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal budget,
            BigDecimal actualSpend,
            Integer discountId,
            Integer expectedBookings,
            BigDecimal revenueBefore,
            BigDecimal revenueDuring,
            long bookingsBefore,
            long bookingsDuring,
            long bookingsWithCode) {
    }

    // US-12: Lay hieu qua cua mot chien luoc
    public Optional<MarketingMetrics> findMetricsByStrategyId(
            int strategyId) {

        String sql = """
                -- Moi booking chi co mot first_paid_at
                WITH first_paid AS (
                    SELECT
                        p.booking_id,
                        MIN(p.paid_at) AS first_paid_at
                    FROM dbo.payments p
                    WHERE p.status = 'Paid'
                      AND p.paid_at IS NOT NULL
                    GROUP BY p.booking_id
                )

                SELECT
                    s.strategy_id,
                    s.title,
                    s.status,
                    s.start_date,
                    s.end_date,
                    s.budget,
                    s.actual_spend,
                    s.discount_id,
                    s.expected_bookings,

                    COALESCE(
                        rb.revenue_before, 0
                    ) AS revenue_before,

                    COALESCE(
                        rd.revenue_during, 0
                    ) AS revenue_during,

                    bb.bookings_before,
                    bd.bookings_during,
                    bc.bookings_with_code

                FROM dbo.marketing_strategies s

                -- 1. Tinh moc thoi gian
                CROSS APPLY (
                    SELECT
                        DATEADD(
                            DAY,
                            -(
                                DATEDIFF(
                                    DAY,
                                    s.start_date,
                                    s.end_date
                                ) + 1
                            ),
                            s.start_date
                        ) AS before_from,

                        DATEADD(
                            DAY,
                            1,
                            s.end_date
                        ) AS during_to
                ) period_dates

                -- 2. Doanh thu truoc chien luoc
                OUTER APPLY (
                    SELECT
                        SUM(p.amount) AS revenue_before
                    FROM dbo.payments p
                    WHERE p.status = 'Paid'
                      AND p.paid_at >= period_dates.before_from
                      AND p.paid_at < s.start_date
                ) rb

                -- 3. Doanh thu trong chien luoc
                OUTER APPLY (
                    SELECT
                        SUM(p.amount) AS revenue_during
                    FROM dbo.payments p
                    WHERE p.status = 'Paid'
                      AND p.paid_at >= s.start_date
                      AND p.paid_at < period_dates.during_to
                ) rd

                -- 4. Booking co first Paid trong ky truoc
                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS bookings_before
                    FROM first_paid f
                    WHERE f.first_paid_at >= period_dates.before_from
                      AND f.first_paid_at < s.start_date
                ) bb

                -- 5. Booking co first Paid trong ky chien luoc
                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS bookings_during
                    FROM first_paid f
                    WHERE f.first_paid_at >= s.start_date
                      AND f.first_paid_at < period_dates.during_to
                ) bd

                -- 6. Booking dung discount lien ket
                -- va co first Paid trong ky chien luoc
                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS bookings_with_code

                    FROM first_paid f

                    JOIN dbo.bookings b
                        ON b.booking_id = f.booking_id

                    WHERE s.discount_id IS NOT NULL
                      AND b.discount_id = s.discount_id
                      AND f.first_paid_at >= s.start_date
                      AND f.first_paid_at < period_dates.during_to
                ) bc

                WHERE s.strategy_id = ?
                """;

        List<MarketingMetrics> results = jdbcTemplate.query(
                sql,

                (rs, rowNum) -> {

                    Date startDate = rs.getDate("start_date");

                    Date endDate = rs.getDate("end_date");

                    return new MarketingMetrics(
                            rs.getInt("strategy_id"),
                            rs.getString("title"),
                            rs.getString("status"),

                            startDate.toLocalDate(),
                            endDate.toLocalDate(),

                            rs.getBigDecimal("budget"),

                            rs.getBigDecimal("actual_spend"),

                            rs.getObject(
                                    "discount_id",
                                    Integer.class),

                            rs.getObject(
                                    "expected_bookings",
                                    Integer.class),

                            rs.getBigDecimal(
                                    "revenue_before"),

                            rs.getBigDecimal(
                                    "revenue_during"),

                            rs.getLong("bookings_before"),

                            rs.getLong("bookings_during"),

                            rs.getLong("bookings_with_code"));
                },

                strategyId);

        return results.stream().findFirst();
    }
}
