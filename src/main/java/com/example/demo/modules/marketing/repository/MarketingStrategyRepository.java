
package com.example.demo.modules.marketing.repository;

import com.example.demo.modules.marketing.dto.CreateMarketingStrategyRequest;
import com.example.demo.modules.marketing.dto.MarketingStrategyResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import java.util.List;
import java.util.Optional;

@Repository
public class MarketingStrategyRepository {

    private final JdbcTemplate jdbcTemplate;

    public MarketingStrategyRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    // Thong tin ma giam gia de kiem tra US-11
    public record DiscountInfo(
            int discountId,
            String code,
            String status,
            java.time.LocalDate validFrom,
            java.time.LocalDate validTo) {
    }

    // 1. Lay vai tro nguoi tao
    public Optional<String> findUserRole(int userId) {

        String sql = """
                SELECT [role] AS user_role
                FROM dbo.users
                WHERE user_id = ?
                """;

        List<String> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("user_role"),
                userId);

        return results.stream().findFirst();
    }

    // 2. Lay thong tin ma giam gia
    public Optional<DiscountInfo> findDiscountById(
            int discountId) {

        String sql = """
                SELECT
                    d.discount_id,
                    d.code,
                    d.status,
                    d.valid_from,
                    d.valid_to

                FROM dbo.discounts d
                    WITH (UPDLOCK, HOLDLOCK)

                WHERE d.discount_id = ?
                """;

        List<DiscountInfo> results = jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new DiscountInfo(
                        rs.getInt("discount_id"),
                        rs.getString("code"),
                        rs.getString("status"),

                        rs.getDate("valid_from")
                                .toLocalDate(),

                        rs.getDate("valid_to")
                                .toLocalDate()),

                discountId);

        return results.stream().findFirst();
    }

    // 3. Tao chien luoc moi voi trang thai Planned
    public int insertStrategy(
            CreateMarketingStrategyRequest request,
            int createdBy) {

        String sql = """
                INSERT INTO dbo.marketing_strategies
                (
                    title,
                    target_segment,
                    description,
                    start_date,
                    end_date,
                    budget,
                    actual_spend,
                    discount_id,
                    expected_bookings,
                    status,
                    created_by
                )
                VALUES
                (
                    ?, ?, ?, ?, ?, ?,
                    0, ?, ?, 'Planned', ?
                )
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int updated = jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS);

            ps.setString(
                    1,
                    request.title().trim());

            if (request.targetSegment() == null) {
                ps.setNull(2, Types.NVARCHAR);
            } else {
                ps.setString(
                        2,
                        request.targetSegment().trim());
            }

            if (request.description() == null) {
                ps.setNull(3, Types.NVARCHAR);
            } else {
                ps.setString(
                        3,
                        request.description());
            }

            ps.setDate(
                    4,
                    Date.valueOf(request.startDate()));

            ps.setDate(
                    5,
                    Date.valueOf(request.endDate()));

            ps.setBigDecimal(
                    6,
                    request.budget());

            // Ma giam gia la tuy chon
            if (request.discountId() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(
                        7,
                        request.discountId());
            }

            // So booking ky vong la tuy chon
            if (request.expectedBookings() == null) {
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(
                        8,
                        request.expectedBookings());
            }

            ps.setInt(9, createdBy);

            return ps;

        }, keyHolder);

        if (updated != 1) {
            throw new IllegalStateException(
                    "Khong the tao chien luoc marketing");
        }

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException(
                    "Khong lay duoc ID chien luoc vua tao");
        }

        return generatedId.intValue();
    }

    // 4. Doc lai chien luoc theo ID
    public Optional<MarketingStrategyResponse> findById(
            int strategyId) {

        String sql = """
                SELECT
                    strategy_id,
                    title,
                    target_segment,
                    description,
                    start_date,
                    end_date,
                    budget,
                    actual_spend,
                    discount_id,
                    expected_bookings,
                    status,
                    created_by,
                    created_at,
                    updated_at

                FROM dbo.marketing_strategies

                WHERE strategy_id = ?
                """;

        List<MarketingStrategyResponse> results = jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new MarketingStrategyResponse(
                        rs.getInt("strategy_id"),
                        rs.getString("title"),
                        rs.getString("target_segment"),
                        rs.getString("description"),

                        rs.getDate("start_date")
                                .toLocalDate(),

                        rs.getDate("end_date")
                                .toLocalDate(),

                        rs.getBigDecimal("budget"),
                        rs.getBigDecimal("actual_spend"),

                        rs.getObject(
                                "discount_id",
                                Integer.class),

                        rs.getObject(
                                "expected_bookings",
                                Integer.class),

                        rs.getString("status"),
                        rs.getInt("created_by"),

                        rs.getTimestamp("created_at")
                                .toLocalDateTime(),

                        rs.getTimestamp("updated_at")
                                .toLocalDateTime()),

                strategyId);

        return results.stream().findFirst();
    }
}
