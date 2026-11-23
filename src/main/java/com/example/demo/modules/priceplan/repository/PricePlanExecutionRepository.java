
package com.example.demo.modules.priceplan.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PricePlanExecutionRepository {

    private final JdbcTemplate jdbcTemplate;

    public PricePlanExecutionRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    // Thong tin ke hoach gia
    public record PricePlanSnapshot(
            int planId,
            int categoryId,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String status,
            LocalDateTime appliedAt) {
    }

    // Thong tin gia cac phong trong mot loai
    public record RoomPriceSnapshot(
            int roomCount,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
    }

    // =====================================
    // 1. KIEM TRA VAI TRO ADMIN
    // =====================================

    public Optional<String> findUserRole(int userId) {

        String sql = """
                SELECT [role]
                FROM dbo.users
                WHERE user_id = ?
                """;

        List<String> roles = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("role"),
                userId);

        return roles.stream().findFirst();
    }

    // =====================================
    // 2. LAY NGAY HIEN TAI TU SQL SERVER
    // =====================================

    public LocalDate getDatabaseToday() {

        Date today = jdbcTemplate.queryForObject(
                "SELECT CAST(GETDATE() AS date)",
                Date.class);

        if (today == null) {
            throw new IllegalStateException(
                    "Khong lay duoc ngay SQL Server");
        }

        return today.toLocalDate();
    }

    // =====================================
    // 3. DOC VA KHOA KE HOACH
    // =====================================

    public Optional<PricePlanSnapshot> lockPlanById(
            int planId) {

        String sql = """
                SELECT
                    plan_id,
                    category_id,
                    old_price,
                    new_price,
                    effective_from,
                    effective_to,
                    status,
                    applied_at
                FROM dbo.price_adjustment_plans
                    WITH (UPDLOCK, HOLDLOCK)
                WHERE plan_id = ?
                """;

        return queryPlan(sql, planId);
    }

    // =====================================
    // 4. KHOA LOAI PHONG
    // =====================================

    public boolean lockCategory(int categoryId) {

        String sql = """
                SELECT category_id
                FROM dbo.categories
                    WITH (UPDLOCK, HOLDLOCK)
                WHERE category_id = ?
                """;

        List<Integer> ids = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getInt("category_id"),
                categoryId);

        return !ids.isEmpty();
    }

    // =====================================
    // 5. KIEM TRA KE HOACH APPLIED KHAC
    // =====================================

    public boolean hasOtherAppliedPlan(
            int categoryId,
            int excludedPlanId) {

        String sql = """
                SELECT COUNT_BIG(*)
                FROM dbo.price_adjustment_plans
                WHERE category_id = ?
                  AND plan_id <> ?
                  AND status = 'Applied'
                """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                categoryId,
                excludedPlanId);

        return count != null && count > 0;
    }

    // =====================================
    // 6. DOC VA KHOA GIA CAC PHONG
    // =====================================

    public RoomPriceSnapshot lockRoomPrices(
            int categoryId) {

        String sql = """
                SELECT price
                FROM dbo.rooms
                    WITH (UPDLOCK, HOLDLOCK)
                WHERE category_id = ?
                ORDER BY room_id
                """;

        List<BigDecimal> prices = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getBigDecimal("price"),
                categoryId);

        if (prices.isEmpty()) {
            return new RoomPriceSnapshot(
                    0, null, null);
        }

        BigDecimal minPrice = prices.get(0);
        BigDecimal maxPrice = prices.get(0);

        for (BigDecimal price : prices) {

            if (price.compareTo(minPrice) < 0) {
                minPrice = price;
            }

            if (price.compareTo(maxPrice) > 0) {
                maxPrice = price;
            }
        }

        return new RoomPriceSnapshot(
                prices.size(),
                minPrice,
                maxPrice);
    }

    // =====================================
    // 7. CAP NHAT GIA CAC PHONG
    // =====================================

    public int updateRoomPrices(
            int categoryId,
            BigDecimal expectedPrice,
            BigDecimal targetPrice) {

        String sql = """
                UPDATE dbo.rooms
                SET price = ?
                WHERE category_id = ?
                  AND price = ?
                """;

        return jdbcTemplate.update(
                sql,
                targetPrice,
                categoryId,
                expectedPrice);
    }

    // =====================================
    // 8. CHUYEN APPROVED -> APPLIED
    // =====================================

    public int markApplied(int planId) {

        String sql = """
                UPDATE dbo.price_adjustment_plans
                SET
                    status = 'Applied',
                    applied_at = GETDATE()
                WHERE plan_id = ?
                  AND status = 'Approved'
                  AND CAST(GETDATE() AS date)
                      >= effective_from
                  AND (
                      effective_to IS NULL
                      OR CAST(GETDATE() AS date)
                          <= effective_to
                  )
                """;

        return jdbcTemplate.update(sql, planId);
    }

    // =====================================
    // 9. CHUYEN APPLIED -> ENDED
    // =====================================

    public int markEnded(int planId) {

        String sql = """
                UPDATE dbo.price_adjustment_plans
                SET status = 'Ended'
                WHERE plan_id = ?
                  AND status = 'Applied'
                  AND applied_at IS NOT NULL
                """;

        return jdbcTemplate.update(sql, planId);
    }

    // =====================================
    // 10. DOC LAI KE HOACH
    // =====================================

    public Optional<PricePlanSnapshot> findById(
            int planId) {

        String sql = """
                SELECT
                    plan_id,
                    category_id,
                    old_price,
                    new_price,
                    effective_from,
                    effective_to,
                    status,
                    applied_at
                FROM dbo.price_adjustment_plans
                WHERE plan_id = ?
                """;

        return queryPlan(sql, planId);
    }

    // =====================================
    // HAM HO TRO CHUYEN RESULTSET
    // =====================================

    private Optional<PricePlanSnapshot> queryPlan(
            String sql,
            int planId) {

        List<PricePlanSnapshot> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {

                    Date effectiveTo = rs.getDate("effective_to");

                    Timestamp appliedAt = rs.getTimestamp("applied_at");

                    return new PricePlanSnapshot(
                            rs.getInt("plan_id"),
                            rs.getInt("category_id"),
                            rs.getBigDecimal("old_price"),
                            rs.getBigDecimal("new_price"),
                            rs.getDate("effective_from")
                                    .toLocalDate(),
                            effectiveTo == null
                                    ? null
                                    : effectiveTo.toLocalDate(),
                            rs.getString("status"),
                            appliedAt == null
                                    ? null
                                    : appliedAt.toLocalDateTime());
                },
                planId);

        return results.stream().findFirst();
    }
}
