
package com.example.demo.modules.marketing.service;

import com.example.demo.modules.marketing.dto.CreateMarketingStrategyRequest;
import com.example.demo.modules.marketing.dto.MarketingStrategyResponse;
import com.example.demo.modules.marketing.repository.MarketingStrategyRepository;
import com.example.demo.modules.marketing.repository.MarketingStrategyRepository.DiscountInfo;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketingStrategyUS11Test {

    private final MarketingStrategyRepository repository = mock(MarketingStrategyRepository.class);

    private final MarketingStrategyService service = new MarketingStrategyService(repository);

    private final LocalDate start = LocalDate.of(2026, 11, 23);

    private final LocalDate end = LocalDate.of(2026, 11, 30);

    // Tao Request mac dinh
    private CreateMarketingStrategyRequest request(
            Integer discountId) {

        return new CreateMarketingStrategyRequest(
                "US11 TEST - Thu hut khach",
                "Sinh vien",
                "Chien dich thu hut khach thue",
                start,
                end,
                new BigDecimal("5000000"),
                discountId,
                40);
    }

    // Gia lap Manager ID = 2
    private void mockManager() {

        when(repository.findUserRole(2))
                .thenReturn(Optional.of("Manager"));
    }

    // Gia lap ma giam gia
    private DiscountInfo discount(
            String status,
            LocalDate validFrom,
            LocalDate validTo) {

        return new DiscountInfo(
                1,
                "THU10",
                status,
                validFrom,
                validTo);
    }

    // Gia lap Response tu database
    private MarketingStrategyResponse response(
            CreateMarketingStrategyRequest req,
            String status) {

        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 22, 22, 30);

        return new MarketingStrategyResponse(
                10,
                req.title(),
                req.targetSegment(),
                req.description(),
                req.startDate(),
                req.endDate(),
                req.budget(),
                BigDecimal.ZERO,
                req.discountId(),
                req.expectedBookings(),
                status,
                2,
                createdAt,
                createdAt);
    }

    // Gia lap INSERT va doc lai thanh cong
    private void mockInsertSuccess(
            CreateMarketingStrategyRequest req) {

        when(repository.insertStrategy(req, 2))
                .thenReturn(10);

        when(repository.findById(10))
                .thenReturn(Optional.of(
                        response(req, "Planned")));
    }

    // TC11-01: Tao khong co discount
    @Test
    void testCreateWithoutDiscount() {

        CreateMarketingStrategyRequest req = request(null);

        mockManager();
        mockInsertSuccess(req);

        MarketingStrategyResponse result = service.createStrategy(req, 2);

        assertEquals(10, result.strategyId());
        assertEquals("Planned", result.status());
        assertEquals(2, result.createdBy());
        assertNull(result.discountId());

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        result.actualSpend()));

        verify(repository, never())
                .findDiscountById(anyInt());

        verify(repository).insertStrategy(req, 2);
    }

    // TC11-02: Discount hop le
    @Test
    void testCreateWithValidDiscount() {

        CreateMarketingStrategyRequest req = request(1);

        mockManager();

        when(repository.findDiscountById(1))
                .thenReturn(Optional.of(
                        discount(
                                "Active",
                                LocalDate.of(2026, 9, 1),
                                end)));

        mockInsertSuccess(req);

        MarketingStrategyResponse result = service.createStrategy(req, 2);

        assertEquals("Planned", result.status());
        assertEquals(1, result.discountId());

        verify(repository).findDiscountById(1);
        verify(repository).insertStrategy(req, 2);
    }

    // TC11-03: Discount khong ton tai
    @Test
    void testDiscountNotFound() {

        mockManager();

        when(repository.findDiscountById(999))
                .thenReturn(Optional.empty());

        assertThrows(
                MarketingStrategyService.MarketingDiscountNotFoundException.class,
                () -> service.createStrategy(
                        request(999), 2));

        verify(repository, never())
                .insertStrategy(any(), anyInt());
    }

    // TC11-04: Discount Disabled
    @Test
    void testDisabledDiscount() {

        mockManager();

        when(repository.findDiscountById(1))
                .thenReturn(Optional.of(
                        discount(
                                "Disabled",
                                start,
                                end)));

        assertThrows(
                MarketingStrategyService.MarketingDiscountConflictException.class,
                () -> service.createStrategy(
                        request(1), 2));

        verify(repository, never())
                .insertStrategy(any(), anyInt());
    }

    // TC11-05: Discount bat dau qua muon
    @Test
    void testDiscountStartsTooLate() {

        mockManager();

        when(repository.findDiscountById(1))
                .thenReturn(Optional.of(
                        discount(
                                "Active",
                                LocalDate.of(2026, 11, 25),
                                end)));

        MarketingStrategyService.MarketingDiscountConflictException error = assertThrows(
                MarketingStrategyService.MarketingDiscountConflictException.class,
                () -> service.createStrategy(
                        request(1), 2));

        assertTrue(
                error.getMessage().contains(
                        "bat dau sau"));

        verify(repository, never())
                .insertStrategy(any(), anyInt());
    }

    // TC11-06: Discount het han qua som
    @Test
    void testDiscountEndsTooEarly() {

        mockManager();

        when(repository.findDiscountById(1))
                .thenReturn(Optional.of(
                        discount(
                                "Active",
                                LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 11, 28))));

        assertThrows(
                MarketingStrategyService.MarketingDiscountConflictException.class,
                () -> service.createStrategy(
                        request(1), 2));

        verify(repository, never())
                .insertStrategy(any(), anyInt());
    }

    // TC11-07: Khong phai Manager
    @Test
    void testNotManager() {

        when(repository.findUserRole(2))
                .thenReturn(Optional.of("Admin"));

        assertThrows(
                MarketingStrategyService.MarketingForbiddenException.class,
                () -> service.createStrategy(
                        request(null), 2));

        when(repository.findUserRole(99))
                .thenReturn(Optional.empty());

        assertThrows(
                MarketingStrategyService.MarketingForbiddenException.class,
                () -> service.createStrategy(
                        request(null), 99));

        verify(repository, never())
                .insertStrategy(any(), anyInt());
    }

    // TC11-08: Manager ID khong hop le
    @Test
    void testInvalidManagerId() {

        assertThrows(
                MarketingStrategyService.MarketingForbiddenException.class,
                () -> service.createStrategy(
                        request(null), 0));

        verifyNoInteractions(repository);
    }

    // TC11-09: Request hoac ten khong hop le
    @Test
    void testInvalidTitleAndRequest() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createStrategy(null, 2));

        CreateMarketingStrategyRequest invalid = new CreateMarketingStrategyRequest(
                "   ",
                "Sinh vien",
                "Mo ta",
                start,
                end,
                BigDecimal.ZERO,
                null,
                10);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createStrategy(
                        invalid, 2));

        verifyNoInteractions(repository);
    }

    // TC11-10: Ngay khong hop le
    @Test
    void testInvalidDates() {

        CreateMarketingStrategyRequest missingStart = new CreateMarketingStrategyRequest(
                "Chien dich",
                null,
                null,
                null,
                end,
                BigDecimal.ZERO,
                null,
                null);

        CreateMarketingStrategyRequest reversed = new CreateMarketingStrategyRequest(
                "Chien dich",
                null,
                null,
                end,
                start,
                BigDecimal.ZERO,
                null,
                null);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createStrategy(
                        missingStart, 2));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createStrategy(
                        reversed, 2));

        verifyNoInteractions(repository);
    }

    // TC11-11: Bat dau va ket thuc cung ngay
    @Test
    void testSameStartAndEndDate() {

        CreateMarketingStrategyRequest req = new CreateMarketingStrategyRequest(
                "Chien dich mot ngay",
                null,
                null,
                start,
                start,
                BigDecimal.ZERO,
                null,
                0);

        mockManager();
        mockInsertSuccess(req);

        MarketingStrategyResponse result = service.createStrategy(req, 2);

        assertEquals(start, result.startDate());
        assertEquals(start, result.endDate());
        assertEquals("Planned", result.status());
    }

    // TC11-12: Ngan sach sai
    @Test
    void testInvalidBudget() {

        BigDecimal[] invalidBudgets = {
                null,
                new BigDecimal("-1"),
                new BigDecimal("5000000.123"),
                new BigDecimal("1000000000000.00")
        };

        for (BigDecimal budget : invalidBudgets) {

            CreateMarketingStrategyRequest req = new CreateMarketingStrategyRequest(
                    "Chien dich",
                    null,
                    null,
                    start,
                    end,
                    budget,
                    null,
                    10);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createStrategy(
                            req, 2));
        }

        verifyNoInteractions(repository);
    }

    // TC11-13: Discount ID va booking ky vong sai
    @Test
    void testInvalidOptionalNumbers() {

        CreateMarketingStrategyRequest invalidDiscount = request(-1);

        CreateMarketingStrategyRequest invalidBookings = new CreateMarketingStrategyRequest(
                "Chien dich",
                null,
                null,
                start,
                end,
                BigDecimal.ZERO,
                null,
                -5);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createStrategy(
                        invalidDiscount, 2));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createStrategy(
                        invalidBookings, 2));

        verifyNoInteractions(repository);
    }

    // TC11-14: Doi tuong muc tieu qua dai
    @Test
    void testTargetSegmentTooLong() {

        CreateMarketingStrategyRequest req = new CreateMarketingStrategyRequest(
                "Chien dich",
                "A".repeat(151),
                null,
                start,
                end,
                BigDecimal.ZERO,
                null,
                10);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createStrategy(req, 2));

        verifyNoInteractions(repository);
    }

    // TC11-15: Repository gap loi SQL Server
    @Test
    void testDatabaseFailure() {

        CreateMarketingStrategyRequest req = request(null);

        mockManager();

        DataAccessResourceFailureException dbError = new DataAccessResourceFailureException(
                "Khong ket noi duoc SQL Server");

        when(repository.insertStrategy(req, 2))
                .thenThrow(dbError);

        DataAccessResourceFailureException actual = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.createStrategy(req, 2));

        assertSame(dbError, actual);

        verify(repository, never())
                .findById(anyInt());
    }

    // TC11-16: Khong doc duoc chien luoc vua tao
    @Test
    void testCreatedStrategyNotFound() {

        CreateMarketingStrategyRequest req = request(null);

        mockManager();

        when(repository.insertStrategy(req, 2))
                .thenReturn(10);

        when(repository.findById(10))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.createStrategy(req, 2));

        verify(repository).findById(10);
    }

    // TC11-17: Trang thai moi khong phai Planned
    @Test
    void testUnexpectedStatus() {

        CreateMarketingStrategyRequest req = request(null);

        mockManager();

        when(repository.insertStrategy(req, 2))
                .thenReturn(10);

        when(repository.findById(10))
                .thenReturn(Optional.of(
                        response(req, "Running")));

        assertThrows(
                IllegalStateException.class,
                () -> service.createStrategy(req, 2));
    }
}
