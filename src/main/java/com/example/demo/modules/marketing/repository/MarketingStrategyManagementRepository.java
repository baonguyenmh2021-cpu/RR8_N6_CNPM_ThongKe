
package com.example.demo.modules.marketing.repository;

import com.example.demo.modules.marketing.dto.MarketingStrategyResponse;
import com.example.demo.modules.marketing.dto.UpdateMarketingStrategyRequest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class MarketingStrategyManagementRepository {

    private final JdbcTemplate jdbcTemplate;

    public MarketingStrategyManagementRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    // Trang thai hien tai cua chien luoc
    public record LockedStrategy(
            int strategyId,
            String status) {
    }

    // Thong tin discount de kiem tra khi cap nhat
    public record DiscountSnapshot(
            int discountId,
            String status,
            LocalDate validFrom,
            LocalDate validTo) {
    }

    // 1. Lay vai tro nguoi thuc hien
    public Optional<String> findUserRole(int userId) {

        String sql = """
                SELECT [role]
                FROM dbo.users
                WHERE user_id = ?
                """;

        List<String> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("role"),
                userId);

        return results.stream().findFirst();
    }

    // 2. Doc va khoa chien luoc can cap nhat
    public Optional<LockedStrategy> lockStrategyById(
            int strategyId) {

        String sql = """
                SELECT
                    strategy_id,
                    status
                FROM dbo.marketing_strategies
                    WITH (UPDLOCK, HOLDLOCK)
                WHERE strategy_id = ?
                """;

        List<LockedStrategy> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new LockedStrategy(
                        rs.getInt("strategy_id"),
                        rs.getString("status")),
                strategyId);

        return results.stream().findFirst();
    }

    // 3. Doc va khoa ma giam gia
    public Optional<DiscountSnapshot> lockDiscountById(
            int discountId) {

        String sql = """
                SELECT
                    discount_id,
                    status,
                    valid_from,
                    valid_to
                FROM dbo.discounts
                    WITH (UPDLOCK, HOLDLOCK)
                WHERE discount_id = ?
                """;

        List<DiscountSnapshot> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DiscountSnapshot(
                        rs.getInt("discount_id"),
                        rs.getString("status"),
                        rs.getDate("valid_from")
                                .toLocalDate(),
                        rs.getDate("valid_to")
                                .toLocalDate()),
                discountId);

        return results.stream().findFirst();
    }

    // 4. Cap nhat noi dung chien luoc
    public int updateStrategyDetails(
            int strategyId,
            UpdateMarketingStrategyRequest request) {

        String sql = """
                UPDATE dbo.marketing_strategies
                SET
                    title = ?,
                    target_segment = ?,
                    description = ?,
                    start_date = ?,
                    end_date = ?,
                    budget = ?,
                    actual_spend = ?,
                    discount_id = ?,
                    expected_bookings = ?
                WHERE strategy_id = ?
                  AND status IN ('Planned', 'Running')
                """;

        return jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setNString(1, request.title());

            setNullableString(
                    ps, 2, request.targetSegment());

            setNullableString(
                    ps, 3, request.description());

            ps.setDate(
                    4,
                    Date.valueOf(request.startDate()));

            ps.setDate(
                    5,
                    Date.valueOf(request.endDate()));

            ps.setBigDecimal(6, request.budget());

            ps.setBigDecimal(7, request.actualSpend());

            setNullableInteger(
                    ps, 8, request.discountId());

            setNullableInteger(
                    ps, 9, request.expectedBookings());

            ps.setInt(10, strategyId);

            return ps;
        });
    }

    // 5. Chuyen trang thai co dieu kien
    public int changeStatus(
            int strategyId,
            String expectedStatus,
            String newStatus) {

        String sql = """
                UPDATE dbo.marketing_strategies
                SET status = ?
                WHERE strategy_id = ?
                  AND status = ?
                  AND (
                      (
                          status = 'Planned'
                          AND ? IN ('Running', 'Cancelled')
                      )
                      OR
                      (
                          status = 'Running'
                          AND ? IN ('Finished', 'Cancelled')
                      )
                  )
                """;

        return jdbcTemplate.update(
                sql,
                newStatus,
                strategyId,
                expectedStatus,
                newStatus,
                newStatus);
    }

    // 6. Doc lai chien luoc sau cap nhat
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

    // Ham ho tro cho cot NVARCHAR NULL
    private static void setNullableString(
            PreparedStatement ps,
            int index,
            String value) throws SQLException {

        if (value == null) {
            ps.setNull(index, Types.NVARCHAR);
        } else {
            ps.setNString(index, value);
        }
    }

    // Ham ho tro cho cot INT NULL
    private static void setNullableInteger(
            PreparedStatement ps,
            int index,
            Integer value) throws SQLException {

        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
