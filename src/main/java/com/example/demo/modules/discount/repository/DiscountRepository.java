
package com.example.demo.modules.discount.repository;

import com.example.demo.modules.discount.dto.CreateDiscountRequest;
import com.example.demo.modules.discount.dto.DiscountResponse;

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
public class DiscountRepository {

    private final JdbcTemplate jdbcTemplate;

    public DiscountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 1. Kiem tra ma giam gia da ton tai hay chua
    // Khong phan biet chu hoa va chu thuong
    public boolean existsByCode(String code) {

        String sql = """
                SELECT COUNT(*)
                FROM dbo.discounts
                WHERE code COLLATE Latin1_General_CI_AS = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql, Integer.class, code);

        return count != null && count > 0;
    }

    // 2. Them ma giam gia vao SQL Server
    public int insertDiscount(
            CreateDiscountRequest request,
            int createdBy) {

        String sql = """
                INSERT INTO dbo.discounts (
                    code,
                    description,
                    [type],
                    [value],
                    min_amount,
                    valid_from,
                    valid_to,
                    usage_limit,
                    created_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, request.code());
            ps.setString(2, request.description());
            ps.setString(3, request.type());
            ps.setBigDecimal(4, request.value());

            BigDecimal minAmount = request.minAmount() == null
                    ? BigDecimal.ZERO
                    : request.minAmount();

            ps.setBigDecimal(5, minAmount);

            ps.setDate(6, Date.valueOf(request.validFrom()));
            ps.setDate(7, Date.valueOf(request.validTo()));

            if (request.usageLimit() == null) {
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(8, request.usageLimit());
            }

            ps.setInt(9, createdBy);

            return ps;

        }, keyHolder);

        Number id = keyHolder.getKey();

        if (id == null) {
            throw new IllegalStateException(
                    "Khong lay duoc ID ma giam gia");
        }

        return id.intValue();
    }

    // 3. Lay ma giam gia theo ID
    public Optional<DiscountResponse> findById(int discountId) {

        String sql = """
                SELECT
                    discount_id,
                    code,
                    description,
                    [type],
                    [value],
                    min_amount,
                    valid_from,
                    valid_to,
                    usage_limit,
                    status,
                    created_by,
                    created_at
                FROM dbo.discounts
                WHERE discount_id = ?
                """;

        List<DiscountResponse> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DiscountResponse(
                        rs.getInt("discount_id"),
                        rs.getString("code"),
                        rs.getString("description"),
                        rs.getString("type"),
                        rs.getBigDecimal("value"),
                        rs.getBigDecimal("min_amount"),
                        rs.getDate("valid_from").toLocalDate(),
                        rs.getDate("valid_to").toLocalDate(),
                        rs.getObject("usage_limit", Integer.class),
                        rs.getString("status"),
                        rs.getInt("created_by"),
                        rs.getTimestamp("created_at")
                                .toLocalDateTime()),
                discountId);

        return results.stream().findFirst();
    }
}
