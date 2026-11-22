
package com.example.demo.modules.occupancy.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class OccupancyRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OccupancyRepository(
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Du lieu tho de Service tinh ty le lap day
    public record OccupancyMetrics(
            int categoryId,
            String categoryName,
            long availableRooms,
            long periodNights,
            long bookedNights,
            BigDecimal paidRevenue,
            BigDecimal minCurrentPrice,
            BigDecimal maxCurrentPrice) {
    }

    // US-08: Lay thong ke theo loai phong
    public List<OccupancyMetrics> findMetrics(
            LocalDate from,
            LocalDate to) {

        String sql = """
                WITH available_rooms AS (
                    SELECT
                        r.room_id,
                        r.category_id,
                        r.price
                    FROM dbo.rooms r
                    WHERE r.status <> 'Maintenance'
                ),

                -- 1. So phong va gia hien tai
                room_summary AS (
                    SELECT
                        category_id,
                        COUNT_BIG(*) AS available_rooms,
                        MIN(price) AS min_price,
                        MAX(price) AS max_price
                    FROM available_rooms
                    GROUP BY category_id
                ),

                -- 2. So dem dat trong khoang thong ke
                booking_summary AS (
                    SELECT
                        r.category_id,

                        SUM(
                            CAST(
                                DATEDIFF(
                                    DAY,

                                    CASE
                                        WHEN b.check_in > :fromDate
                                            THEN b.check_in
                                        ELSE :fromDate
                                    END,

                                    CASE
                                        WHEN b.check_out < :toDate
                                            THEN b.check_out
                                        ELSE :toDate
                                    END

                                ) AS BIGINT
                            )
                        ) AS booked_nights

                    FROM dbo.bookings b

                    JOIN available_rooms r
                        ON r.room_id = b.room_id

                    WHERE b.status <> 'Cancelled'
                      AND b.check_in < :toDate
                      AND b.check_out > :fromDate

                    GROUP BY r.category_id
                ),

                -- 3. Doanh thu Paid theo paid_at
                revenue_summary AS (
                    SELECT
                        r.category_id,
                        SUM(p.amount) AS paid_revenue

                    FROM dbo.payments p

                    JOIN dbo.bookings b
                        ON b.booking_id = p.booking_id

                    JOIN dbo.rooms r
                        ON r.room_id = b.room_id

                    WHERE p.status = 'Paid'
                      AND p.paid_at >= :fromDate
                      AND p.paid_at < :toDate

                    GROUP BY r.category_id
                )

                -- 4. Ghep ket qua theo loai phong
                SELECT
                    c.category_id,
                    c.name AS category_name,

                    COALESCE(
                        rs.available_rooms, 0
                    ) AS available_rooms,

                    DATEDIFF(
                        DAY, :fromDate, :toDate
                    ) AS period_nights,

                    COALESCE(
                        bs.booked_nights, 0
                    ) AS booked_nights,

                    COALESCE(
                        rv.paid_revenue, 0
                    ) AS paid_revenue,

                    rs.min_price,
                    rs.max_price

                FROM dbo.categories c

                LEFT JOIN room_summary rs
                    ON rs.category_id = c.category_id

                LEFT JOIN booking_summary bs
                    ON bs.category_id = c.category_id

                LEFT JOIN revenue_summary rv
                    ON rv.category_id = c.category_id

                ORDER BY c.category_id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(
                        "fromDate",
                        Date.valueOf(from))
                .addValue(
                        "toDate",
                        Date.valueOf(to));

        return jdbcTemplate.query(
                sql,
                params,

                (rs, rowNum) -> new OccupancyMetrics(
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getLong("available_rooms"),
                        rs.getLong("period_nights"),
                        rs.getLong("booked_nights"),
                        rs.getBigDecimal("paid_revenue"),
                        rs.getBigDecimal("min_price"),
                        rs.getBigDecimal("max_price")));
    }
}
