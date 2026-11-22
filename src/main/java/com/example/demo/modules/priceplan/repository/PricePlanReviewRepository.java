
package com.example.demo.modules.priceplan.repository;

import com.example.demo.modules.priceplan.dto.ReviewPricePlanResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class PricePlanReviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public PricePlanReviewRepository(
            JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Du lieu ke hoach can xet duyet
    public record PlanForReview(
            int planId,
            int categoryId,
            int createdBy,
            String status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {
    }

    // 1. Lay vai tro cua nguoi dung
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

    // 2. Xac dinh loai phong cua ke hoach
    public Optional<Integer> findPlanCategoryId(int planId) {

        String sql = """
                SELECT category_id
                FROM dbo.price_adjustment_plans
                WHERE plan_id = ?
                """;

        List<Integer> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getInt("category_id"),
                planId);

        return results.stream().findFirst();
    }

    // 3. Khoa xet duyet theo loai phong
    // PHAI goi ben trong @Transactional
    public void lockCategoryForReview(int categoryId) {

        String sql = """
                SET NOCOUNT ON;

                DECLARE @resource NVARCHAR(255) =
                    CONCAT(
                        N'RR8-US10-Category-',
                        CONVERT(NVARCHAR(20), ?)
                    );

                DECLARE @lockResult INT;

                EXEC @lockResult = sys.sp_getapplock
                    @Resource = @resource,
                    @LockMode = 'Exclusive',
                    @LockOwner = 'Transaction',
                    @LockTimeout = 10000;

                SELECT @lockResult;
                """;

        Integer result = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                categoryId);

        if (result == null || result < 0) {
            throw new IllegalStateException(
                    "Khong the khoa xet duyet loai phong");
        }
    }

    // 4. Doc va khoa ke hoach can xet duyet
    public Optional<PlanForReview> findPlanForReview(
            int planId) {

        String sql = """
                SELECT
                    plan_id,
                    category_id,
                    created_by,
                    status,
                    effective_from,
                    effective_to

                FROM dbo.price_adjustment_plans
                    WITH (UPDLOCK, HOLDLOCK)

                WHERE plan_id = ?
                """;

        List<PlanForReview> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {

                    Date endDate = rs.getDate("effective_to");

                    return new PlanForReview(
                            rs.getInt("plan_id"),
                            rs.getInt("category_id"),
                            rs.getInt("created_by"),
                            rs.getString("status"),
                            rs.getDate("effective_from")
                                    .toLocalDate(),
                            endDate == null
                                    ? null
                                    : endDate.toLocalDate());
                },
                planId);

        return results.stream().findFirst();
    }

    // 5. Kiem tra ke hoach Approved/Applied
    // co thoi gian giao nhau hay khong
    public boolean hasOverlappingPlan(
            int categoryId,
            int excludedPlanId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {

        LocalDate endDate = effectiveTo == null
                ? LocalDate.of(9999, 12, 31)
                : effectiveTo;

        String sql = """
                SELECT CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM dbo.price_adjustment_plans

                        WHERE category_id = ?
                          AND plan_id <> ?

                          AND status IN (
                              'Approved',
                              'Applied'
                          )

                          AND effective_from <= ?

                          AND (
                              effective_to IS NULL
                              OR effective_to >= ?
                          )
                    )
                    THEN 1
                    ELSE 0
                END
                """;

        Integer result = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                categoryId,
                excludedPlanId,
                Date.valueOf(endDate),
                Date.valueOf(effectiveFrom));

        return result != null && result == 1;
    }

    // 6. Cap nhat ket qua xet duyet
    // Chi cho phep cap nhat Draft
    public boolean updateReview(
            int planId,
            int adminId,
            boolean approve,
            String rejectReason) {

        String newStatus = approve
                ? "Approved"
                : "Rejected";

        String sql = """
                UPDATE dbo.price_adjustment_plans
                SET
                    status = ?,
                    approved_by = ?,
                    approved_at = GETDATE(),
                    reject_reason = ?

                WHERE plan_id = ?
                  AND status = 'Draft'
                  AND created_by <> ?
                """;

        int updated = jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, newStatus);
                    ps.setInt(2, adminId);

                    if (approve) {
                        ps.setNull(3, Types.NVARCHAR);
                    } else {
                        ps.setString(3, rejectReason);
                    }

                    ps.setInt(4, planId);
                    ps.setInt(5, adminId);
                });

        return updated == 1;
    }

    // 7. Doc lai ket qua da xet duyet
    public Optional<ReviewPricePlanResponse> findReviewResult(
            int planId) {

        String sql = """
                SELECT
                    plan_id,
                    title,
                    category_id,
                    effective_from,
                    effective_to,
                    status,
                    created_by,
                    approved_by,
                    approved_at,
                    reject_reason

                FROM dbo.price_adjustment_plans

                WHERE plan_id = ?
                """;

        List<ReviewPricePlanResponse> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {

                    Date endDate = rs.getDate("effective_to");

                    Timestamp approvedAt = rs.getTimestamp("approved_at");

                    Integer approvedBy = rs.getObject(
                            "approved_by",
                            Integer.class);

                    return new ReviewPricePlanResponse(
                            rs.getInt("plan_id"),
                            rs.getString("title"),
                            rs.getInt("category_id"),
                            rs.getDate("effective_from")
                                    .toLocalDate(),
                            endDate == null
                                    ? null
                                    : endDate.toLocalDate(),
                            rs.getString("status"),
                            rs.getInt("created_by"),
                            approvedBy,
                            approvedAt == null
                                    ? null
                                    : approvedAt.toLocalDateTime(),
                            rs.getString("reject_reason"));
                },
                planId);

        return results.stream().findFirst();
    }
}
