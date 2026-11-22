
package com.example.demo.modules.priceplan.service;

import com.example.demo.modules.priceplan.dto.CreatePricePlanRequest;
import com.example.demo.modules.priceplan.dto.PricePlanResponse;
import com.example.demo.modules.priceplan.repository.PricePlanRepository;
import com.example.demo.modules.priceplan.repository.PricePlanRepository.CategoryPriceInfo;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PricePlanUS09Test {

    private final PricePlanRepository repository = mock(PricePlanRepository.class);

    private final PricePlanService service = new PricePlanService(repository);

    private final LocalDate from = LocalDate.of(2026, 12, 1);

    private final LocalDate to = LocalDate.of(2026, 12, 31);

    // Du lieu Request hop le
    private CreatePricePlanRequest validRequest() {
        return new CreatePricePlanRequest(
                "Giam gia Standard",
                1,
                new BigDecimal("360000"),
                from,
                to,
                "Tang ty le lap day");
    }

    // Gia lap nguoi dung la Manager
    private void mockManager() {
        when(repository.findUserRole(2))
                .thenReturn(Optional.of("Manager"));
    }

    // Gia lap loai phong co 3 phong dong gia
    private void mockUniformPrices() {
        when(repository.findCategoryPriceInfo(1))
                .thenReturn(Optional.of(
                        new CategoryPriceInfo(
                                3,
                                new BigDecimal("400000"),
                                new BigDecimal("400000"))));
    }

    // Du lieu Response mau
    private PricePlanResponse response(
            CreatePricePlanRequest request,
            String status) {

        return new PricePlanResponse(
                7,
                request.title(),
                request.categoryId(),
                new BigDecimal("400000"),
                request.newPrice(),
                request.effectiveFrom(),
                request.effectiveTo(),
                request.reason(),
                status,
                2,
                LocalDateTime.of(2026, 11, 22, 19, 0));
    }

    // Gia lap tao va doc lai thanh cong
    private void mockInsertSuccess(
            CreatePricePlanRequest request) {

        when(repository.insertDraft(
                request,
                new BigDecimal("400000"),
                2)).thenReturn(7);

        when(repository.findById(7))
                .thenReturn(Optional.of(
                        response(request, "Draft")));
    }

    // TC09-01: Tao ke hoach thanh cong
    @Test
    void testCreatePricePlanSuccess() {

        CreatePricePlanRequest request = validRequest();

        mockManager();
        mockUniformPrices();
        mockInsertSuccess(request);

        PricePlanResponse result = service.createPricePlan(request, 2);

        assertEquals(7, result.planId());
        assertEquals("Draft", result.status());
        assertEquals(2, result.createdBy());

        assertEquals(
                0,
                new BigDecimal("400000")
                        .compareTo(result.oldPrice()));

        assertEquals(
                0,
                new BigDecimal("360000")
                        .compareTo(result.newPrice()));

        verify(repository).insertDraft(
                request,
                new BigDecimal("400000"),
                2);
    }

    // TC09-02: Khong nhap ngay ket thuc
    @Test
    void testWithoutEffectiveTo() {

        CreatePricePlanRequest request = new CreatePricePlanRequest(
                "Tang gia Standard",
                1,
                new BigDecimal("450000"),
                from,
                null,
                "Nhu cau phong tang");

        mockManager();
        mockUniformPrices();
        mockInsertSuccess(request);

        PricePlanResponse result = service.createPricePlan(request, 2);

        assertEquals("Draft", result.status());
        assertNull(result.effectiveTo());
    }

    // TC09-03: Ngay bat dau bang ngay ket thuc
    @Test
    void testSameEffectiveDates() {

        CreatePricePlanRequest request = new CreatePricePlanRequest(
                "Dieu chinh gia trong ngay",
                1,
                new BigDecimal("450000"),
                from,
                from,
                "Dieu chinh gia theo ke hoach");

        mockManager();
        mockUniformPrices();
        mockInsertSuccess(request);

        PricePlanResponse result = service.createPricePlan(request, 2);

        assertEquals(from, result.effectiveFrom());
        assertEquals(from, result.effectiveTo());
        assertEquals("Draft", result.status());
    }

    // TC09-04: Admin khong duoc tao ke hoach
    @Test
    void testAdminCannotCreate() {

        when(repository.findUserRole(2))
                .thenReturn(Optional.of("Admin"));

        assertThrows(
                PricePlanService.PricePlanForbiddenException.class,
                () -> service.createPricePlan(
                        validRequest(), 2));

        verify(repository, never())
                .findCategoryPriceInfo(anyInt());
    }

    // TC09-05: Nguoi lap khong ton tai
    @Test
    void testManagerNotFound() {

        when(repository.findUserRole(99))
                .thenReturn(Optional.empty());

        assertThrows(
                PricePlanService.PricePlanForbiddenException.class,
                () -> service.createPricePlan(
                        validRequest(), 99));
    }

    // TC09-06: ID Manager khong hop le
    @Test
    void testInvalidManagerId() {

        assertThrows(
                PricePlanService.PricePlanForbiddenException.class,
                () -> service.createPricePlan(
                        validRequest(), 0));

        verifyNoInteractions(repository);
    }

    // TC09-07: Loai phong khong ton tai
    @Test
    void testCategoryNotFound() {

        mockManager();

        when(repository.findCategoryPriceInfo(1))
                .thenReturn(Optional.empty());

        assertThrows(
                PricePlanService.PricePlanNotFoundException.class,
                () -> service.createPricePlan(
                        validRequest(), 2));

        verify(repository, never())
                .insertDraft(any(), any(), anyInt());
    }

    // TC09-08: Loai phong chua co phong
    @Test
    void testCategoryWithoutRooms() {

        mockManager();

        when(repository.findCategoryPriceInfo(1))
                .thenReturn(Optional.of(
                        new CategoryPriceInfo(
                                0, null, null)));

        assertThrows(
                IllegalStateException.class,
                () -> service.createPricePlan(
                        validRequest(), 2));

        verify(repository, never())
                .insertDraft(any(), any(), anyInt());
    }

    // TC09-09: Phong cung loai khong dong gia
    @Test
    void testDifferentRoomPrices() {

        mockManager();

        when(repository.findCategoryPriceInfo(1))
                .thenReturn(Optional.of(
                        new CategoryPriceInfo(
                                3,
                                new BigDecimal("400000"),
                                new BigDecimal("450000"))));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.createPricePlan(
                        validRequest(), 2));

        assertTrue(
                error.getMessage().contains(
                        "khong dong nhat"));

        verify(repository, never())
                .insertDraft(any(), any(), anyInt());
    }

    // TC09-10: Gia moi khong hop le
    @Test
    void testInvalidNewPrice() {

        for (BigDecimal price : new BigDecimal[] {
                null,
                BigDecimal.ZERO,
                new BigDecimal("-1000"),
                new BigDecimal("360000.123"),
                new BigDecimal("10000000000.00")
        }) {

            CreatePricePlanRequest request = new CreatePricePlanRequest(
                    "Giam gia Standard",
                    1,
                    price,
                    from,
                    to,
                    "Tang ty le lap day");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createPricePlan(
                            request, 2));
        }

        verifyNoInteractions(repository);
    }

    // TC09-11: Khoang ngay khong hop le
    @Test
    void testInvalidDateRange() {

        CreatePricePlanRequest missingFrom = new CreatePricePlanRequest(
                "Giam gia",
                1,
                new BigDecimal("360000"),
                null,
                to,
                "Tang ty le lap day");

        CreatePricePlanRequest reversed = new CreatePricePlanRequest(
                "Giam gia",
                1,
                new BigDecimal("360000"),
                to,
                from,
                "Tang ty le lap day");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createPricePlan(
                        missingFrom, 2));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createPricePlan(
                        reversed, 2));

        verifyNoInteractions(repository);
    }

    // TC09-12: Ten, ly do, categoryId sai
    @Test
    void testInvalidBasicInformation() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createPricePlan(
                        null, 2));

        CreatePricePlanRequest emptyTitle = new CreatePricePlanRequest(
                "   ", 1,
                new BigDecimal("360000"),
                from, to,
                "Tang ty le lap day");

        CreatePricePlanRequest emptyReason = new CreatePricePlanRequest(
                "Giam gia", 1,
                new BigDecimal("360000"),
                from, to,
                "   ");

        CreatePricePlanRequest invalidCategory = new CreatePricePlanRequest(
                "Giam gia", 0,
                new BigDecimal("360000"),
                from, to,
                "Tang ty le lap day");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createPricePlan(
                        emptyTitle, 2));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createPricePlan(
                        emptyReason, 2));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createPricePlan(
                        invalidCategory, 2));

        verifyNoInteractions(repository);
    }

    // TC09-13: SQL Server gap loi khi INSERT
    @Test
    void testInsertDatabaseFailure() {

        CreatePricePlanRequest request = validRequest();

        mockManager();
        mockUniformPrices();

        DataAccessResourceFailureException error = new DataAccessResourceFailureException(
                "Khong ket noi duoc SQL Server");

        when(repository.insertDraft(
                request,
                new BigDecimal("400000"),
                2)).thenThrow(error);

        DataAccessResourceFailureException actual = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.createPricePlan(
                        request, 2));

        assertSame(error, actual);

        verify(repository, never())
                .findById(anyInt());
    }

    // TC09-14: Khong doc duoc ke hoach vua tao
    @Test
    void testCreatedPlanNotFound() {

        CreatePricePlanRequest request = validRequest();

        mockManager();
        mockUniformPrices();

        when(repository.insertDraft(
                request,
                new BigDecimal("400000"),
                2)).thenReturn(7);

        when(repository.findById(7))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.createPricePlan(
                        request, 2));

        verify(repository).findById(7);
    }

    // TC09-15: Ke hoach moi khong phai Draft
    @Test
    void testUnexpectedPlanStatus() {

        CreatePricePlanRequest request = validRequest();

        mockManager();
        mockUniformPrices();

        when(repository.insertDraft(
                request,
                new BigDecimal("400000"),
                2)).thenReturn(7);

        when(repository.findById(7))
                .thenReturn(Optional.of(
                        response(request, "Approved")));

        assertThrows(
                IllegalStateException.class,
                () -> service.createPricePlan(
                        request, 2));
    }
}
