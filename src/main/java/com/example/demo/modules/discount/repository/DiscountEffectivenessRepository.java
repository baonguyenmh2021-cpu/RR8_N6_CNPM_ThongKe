
package com.example.demo.modules.discount.repository;

import com.example.demo.modules.discount.dto.DiscountEffectivenessResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

@Repository
public class DiscountEffectivenessRepository {

    private final JdbcTemplate jdbcTemplate;

    public DiscountEffectivenessRepository(
            JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // US-06: Thong ke hieu qua ma giam gia
    // from, to: ngay tao booking, khoang [from, to)
    // Null ca hai tham so: thong ke tat ca

    public List<DiscountEffectivenessResponse> findEffectiveness(
            LocalDate from,
            LocalDate to) {

        String sql = """
                WITH filtered_bookings AS (
                    SELECT
                        booking_id,
                        discount_id,
                        discount_amount,
                        status
                    FROM dbo.bookings
                    WHERE
                        (? IS NULL OR created_at >= ?)
                        AND
                        (? IS NULL OR created_at < ?)
                ),

                usage_summary AS (
                    SELECT
                        discount_id,
                        COUNT(*) AS uses,
                        COALESCE(
                            SUM(discount_amount), 0
                        ) AS total_discount
                    FROM filtered_bookings
                    WHERE
                        discount_id IS NOT NULL
                        AND status <> 'Cancelled'
                    GROUP BY discount_id
                ),

                revenue_summary AS (
                    SELECT
                        b.discount_id,
                        COALESCE(
                            SUM(p.amount), 0
                        ) AS net_revenue
                    FROM filtered_bookings b
                    JOIN dbo.payments p
                        ON p.booking_id = b.booking_id
                    WHERE
                        b.discount_id IS NOT NULL
                        AND p.status = 'Paid'
                    GROUP BY b.discount_id
                )

                SELECT
                    d.discount_id,
                    d.code,

                    CASE
                        WHEN d.status = 'Disabled'
                            THEN 'Disabled'
                        WHEN CAST(GETDATE() AS DATE) > d.valid_to
                            THEN 'Expired'
                        ELSE 'Active'
                    END AS effective_status,

                    d.usage_limit,

                    COALESCE(u.uses, 0) AS uses,

                    CASE
                        WHEN d.usage_limit IS NULL
                            THEN NULL
                        WHEN COALESCE(u.uses, 0) >= d.usage_limit
                            THEN 0
                        ELSE
                            d.usage_limit - COALESCE(u.uses, 0)
                    END AS remaining_uses,

                    COALESCE(
                        u.total_discount, 0
                    ) AS total_discount,

                    COALESCE(
                        r.net_revenue, 0
                    ) AS net_revenue

                FROM dbo.discounts d

                LEFT JOIN usage_summary u
                    ON u.discount_id = d.discount_id

                LEFT JOIN revenue_summary r
                    ON r.discount_id = d.discount_id

                ORDER BY
                    net_revenue DESC,
                    d.code ASC
                """;

        return jdbcTemplate.query(
                sql,

                ps -> {
                    if (from == null) {
                        ps.setNull(1, Types.TIMESTAMP);
                        ps.setNull(2, Types.TIMESTAMP);
                    } else {
                        Timestamp start = Timestamp.valueOf(
                                from.atStartOfDay());

                        ps.setTimestamp(1, start);
                        ps.setTimestamp(2, start);
                    }

                    if (to == null) {
                        ps.setNull(3, Types.TIMESTAMP);
                        ps.setNull(4, Types.TIMESTAMP);
                    } else {
                        Timestamp end = Timestamp.valueOf(
                                to.atStartOfDay());

                        ps.setTimestamp(3, end);
                        ps.setTimestamp(4, end);
                    }
                },

                (rs, rowNum) -> new DiscountEffectivenessResponse(
                        rs.getInt("discount_id"),
                        rs.getString("code"),
                        rs.getString("effective_status"),
                        rs.getObject("usage_limit", Integer.class),
                        rs.getLong("uses"),
                        rs.getObject("remaining_uses", Integer.class),
                        rs.getBigDecimal("total_discount"),
                        rs.getBigDecimal("net_revenue")));
    }
}
