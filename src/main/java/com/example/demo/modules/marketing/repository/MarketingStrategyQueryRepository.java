
package com.example.demo.modules.marketing.repository;

import com.example.demo.modules.marketing.dto.MarketingStrategySummaryResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MarketingStrategyQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public MarketingStrategyQueryRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    // Cac cot can hien thi o danh sach
    private static final String BASE_SQL = """
            SELECT
                s.strategy_id,
                s.title,
                s.target_segment,
                s.start_date,
                s.end_date,
                s.budget,
                s.status
            FROM dbo.marketing_strategies s
            """;

    // Sap xep theo ngay bat dau va ID
    private static final String ORDER_SQL = """
            ORDER BY
                s.start_date ASC,
                s.strategy_id ASC
            """;

    // Chuyen du lieu SQL thanh DTO
    private static final RowMapper<MarketingStrategySummaryResponse> ROW_MAPPER = (rs, rowNum) ->

    new MarketingStrategySummaryResponse(
            rs.getInt("strategy_id"),
            rs.getString("title"),
            rs.getString("target_segment"),
            rs.getDate("start_date").toLocalDate(),
            rs.getDate("end_date").toLocalDate(),
            rs.getBigDecimal("budget"),
            rs.getString("status"));

    // US-13: Lay danh sach theo trang thai tuy chon
    public List<MarketingStrategySummaryResponse> findStrategies(
            String status) {

        // Khong truyen status -> lay tat ca
        if (status == null) {

            String sql = BASE_SQL + ORDER_SQL;

            return jdbcTemplate.query(
                    sql,
                    ROW_MAPPER);
        }

        // Co status -> loc theo trang thai
        String sql = BASE_SQL
                + " WHERE s.status = ? "
                + ORDER_SQL;

        return jdbcTemplate.query(
                sql,
                ROW_MAPPER,
                status);
    }
}
