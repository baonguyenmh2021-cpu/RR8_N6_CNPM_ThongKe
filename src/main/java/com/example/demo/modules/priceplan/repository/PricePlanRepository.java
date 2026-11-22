
package com.example.demo.modules.priceplan.repository;

import com.example.demo.modules.priceplan.dto.CreatePricePlanRequest;
import com.example.demo.modules.priceplan.dto.PricePlanResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class PricePlanRepository {

    private final JdbcTemplate jdbcTemplate;

    public PricePlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Du lieu gia hien tai cua mot loai phong
    public record CategoryPriceInfo(
            long roomCount,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
    }

    // 1. Lay vai tro cua nguoi lap ke hoach
    public Optional<String> findUserRole(int userId) {

        String sql = """
                SELECT [role] AS user_role
                FROM dbo.users
                WHERE user_id = ?
                """;

        List<String> roles = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("user_role"),
                userId);

        return roles.stream().findFirst();
    }

    // 2. Kiem tra gia hien tai cua loai phong
    public Optional<CategoryPriceInfo> findCategoryPriceInfo(
            int categoryId) {

        String sql = """
                SELECT
                    c.category_id,

                    COUNT_BIG(r.room_id) AS room_count,

                    MIN(r.price) AS min_price,

                    MAX(r.price) AS max_price

                FROM dbo.categories c

                LEFT JOIN dbo.rooms r WITH (UPDLOCK, HOLDLOCK)
                    ON r.category_id = c.category_id

                WHERE c.category_id = ?

                GROUP BY c.category_id
                """;

        List<CategoryPriceInfo> results = jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new CategoryPriceInfo(
                        rs.getLong("room_count"),
                        rs.getBigDecimal("min_price"),
                        rs.getBigDecimal("max_price")),

                categoryId);

        return results.stream().findFirst();
    }

    // 3. Luu ke hoach moi vao SQL Server
    public int insertDraft(
            CreatePricePlanRequest request,
            BigDecimal oldPrice,
            int createdBy) {

        String sql = """
                INSERT INTO dbo.price_adjustment_plans
                (
                    title,
                    category_id,
                    old_price,
                    new_price,
                    effective_from,
                    effective_to,
                    reason,
                    status,
                    created_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'Draft', ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, request.title().trim());

            ps.setInt(2, request.categoryId());

            ps.setBigDecimal(3, oldPrice);

            ps.setBigDecimal(4, request.newPrice());

            ps.setDate(
                    5,
                    Date.valueOf(request.effectiveFrom()));

            if (request.effectiveTo() != null) {
                ps.setDate(
                        6,
                        Date.valueOf(request.effectiveTo()));
            } else {
                ps.setNull(6, Types.DATE);
            }

            ps.setString(7, request.reason().trim());

            ps.setInt(8, createdBy);

            return ps;

        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException(
                    "Khong lay duoc ID ke hoach vua tao");
        }

        return generatedId.intValue();
    }

    // 4. Lay ke hoach theo ID
    public Optional<PricePlanResponse> findById(int planId) {

        String sql = """
                SELECT
                    plan_id,
                    title,
                    category_id,
                    old_price,
                    new_price,
                    effective_from,
                    effective_to,
                    reason,
                    status,
                    created_by,
                    created_at

                FROM dbo.price_adjustment_plans

                WHERE plan_id = ?
                """;

        List<PricePlanResponse> results = jdbcTemplate.query(
                sql,

                (rs, rowNum) -> {

                    Date effectiveTo = rs.getDate("effective_to");

                    return new PricePlanResponse(
                            rs.getInt("plan_id"),
                            rs.getString("title"),
                            rs.getInt("category_id"),
                            rs.getBigDecimal("old_price"),
                            rs.getBigDecimal("new_price"),

                            rs.getDate("effective_from")
                                    .toLocalDate(),

                            effectiveTo == null
                                    ? null
                                    : effectiveTo.toLocalDate(),

                            rs.getString("reason"),
                            rs.getString("status"),
                            rs.getInt("created_by"),

                            rs.getTimestamp("created_at")
                                    .toLocalDateTime());
                },

                planId);

        return results.stream().findFirst();
    }
}
