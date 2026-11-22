
package com.example.demo.modules.discount.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class DiscountManagementRepository {

    private final JdbcTemplate jdbcTemplate;

    public DiscountManagementRepository(
            JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Thong tin ma giam gia va so luot da su dung
    public record DiscountState(
            int discountId,
            String code,
            String storedStatus,
            LocalDate validFrom,
            LocalDate validTo,
            BigDecimal minAmount,
            Integer usageLimit,
            long uses,
            LocalDate databaseToday) {
    }

    // 1. Lay thong tin ma giam gia theo ID
    public Optional<DiscountState> findById(
            int discountId) {

        String sql = """
                SELECT
                    d.discount_id,
                    d.code,
                    d.status,
                    d.valid_from,
                    d.valid_to,
                    d.min_amount,
                    d.usage_limit,

                    CAST(GETDATE() AS DATE) AS db_today,

                    (
                        SELECT COUNT_BIG(*)
                        FROM dbo.bookings b
                        WHERE b.discount_id = d.discount_id
                          AND b.status <> 'Cancelled'
                    ) AS uses

                FROM dbo.discounts d
                WHERE d.discount_id = ?
                """;

        List<DiscountState> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DiscountState(
                        rs.getInt("discount_id"),
                        rs.getString("code"),
                        rs.getString("status"),
                        rs.getDate("valid_from").toLocalDate(),
                        rs.getDate("valid_to").toLocalDate(),
                        rs.getBigDecimal("min_amount"),
                        rs.getObject("usage_limit", Integer.class),
                        rs.getLong("uses"),
                        rs.getDate("db_today").toLocalDate()),
                discountId);

        return results.stream().findFirst();
    }

    // 2. Vo hieu hoa ma giam gia
    public boolean disableDiscount(int discountId) {

        String sql = """
                UPDATE dbo.discounts
                SET status = 'Disabled'
                WHERE discount_id = ?
                  AND status = 'Active'
                """;

        int rows = jdbcTemplate.update(
                sql,
                discountId);

        return rows > 0;
    }

    // 3. Gia han ma giam gia
    public boolean extendDiscount(
            int discountId,
            LocalDate newValidTo) {

        String sql = """
                UPDATE dbo.discounts
                SET valid_to = ?
                WHERE discount_id = ?
                  AND valid_to < ?
                """;

        Date newDate = Date.valueOf(newValidTo);

        int rows = jdbcTemplate.update(
                sql,
                newDate,
                discountId,
                newDate);

        return rows > 0;
    }
}
