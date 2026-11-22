
package com.example.demo.modules.marketing.service;

import com.example.demo.modules.marketing.dto.ChangeMarketingStrategyStatusRequest;
import com.example.demo.modules.marketing.dto.MarketingStrategyResponse;
import com.example.demo.modules.marketing.dto.UpdateMarketingStrategyRequest;

import com.example.demo.modules.marketing.repository.MarketingStrategyManagementRepository;
import com.example.demo.modules.marketing.repository.MarketingStrategyManagementRepository.DiscountSnapshot;
import com.example.demo.modules.marketing.repository.MarketingStrategyManagementRepository.LockedStrategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketingUS14Test {

    @Mock
    private MarketingStrategyManagementRepository repository;

    @InjectMocks
    private MarketingStrategyManagementService service;

    private static final int STRATEGY_ID = 7;
    private static final int MANAGER_ID = 2;

    private static final LocalDate START = LocalDate.of(2026, 11, 23);

    private static final LocalDate END = LocalDate.of(2026, 11, 30);

    // =========================================
    // DU LIEU GIA LAP
    // =========================================

    private UpdateMarketingStrategyRequest request(
            Integer discountId) {

        return new UpdateMarketingStrategyRequest(
                "US14 TEST - Uu dai mua thu",
                "Sinh vien",
                "Cap nhat chien luoc marketing",
                START,
                END,
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                discountId,
                40);
    }

    private UpdateMarketingStrategyRequest customRequest(
            String title,
            String targetSegment,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal budget,
            BigDecimal actualSpend,
            Integer discountId,
            Integer expectedBookings) {

        return new UpdateMarketingStrategyRequest(
                title,
                targetSegment,
                "Mo ta kiem thu",
                startDate,
                endDate,
                budget,
                actualSpend,
                discountId,
                expectedBookings);
    }

    private MarketingStrategyResponse response(
            String status,
            Integer discountId) {

        LocalDateTime now = LocalDateTime.of(2026, 11, 23, 10, 0);

        return new MarketingStrategyResponse(
                STRATEGY_ID,
                "US14 TEST - Uu dai mua thu",
                "Sinh vien",
                "Cap nhat chien luoc marketing",
                START,
                END,
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                discountId,
                40,
                status,
                MANAGER_ID,
                now,
                now);
    }

    private void mockManager() {

        when(repository.findUserRole(MANAGER_ID))
                .thenReturn(Optional.of("Manager"));
    }

    private void mockLockedStrategy(String status) {

        when(repository.lockStrategyById(STRATEGY_ID))
                .thenReturn(Optional.of(
                        new LockedStrategy(
                                STRATEGY_ID,
                                status)));
    }

    private void mockValidDiscount() {

        when(repository.lockDiscountById(1))
                .thenReturn(Optional.of(
                        new DiscountSnapshot(
                                1,
                                "Active",
                                START,
                                END)));
    }

    // =========================================
    // NHOM A - CAP NHAT NOI DUNG
    // =========================================

    // TC14-01: Cap nhat Planned thanh cong
    @Test
    void testUpdatePlannedSuccessfully() {

        UpdateMarketingStrategyRequest req = request(null);

        mockManager();
        mockLockedStrategy("Planned");

        when(repository.updateStrategyDetails(
                STRATEGY_ID, req))
                .thenReturn(1);

        when(repository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(
                        response("Planned", null)));

        MarketingStrategyResponse result = service.updateDetails(
                STRATEGY_ID,
                req,
                MANAGER_ID);

        assertEquals("Planned", result.status());
        assertNull(result.discountId());

        assertEquals(
                0,
                req.budget().compareTo(result.budget()));

        verify(repository).updateStrategyDetails(
                STRATEGY_ID, req);

        verify(repository, never())
                .lockDiscountById(anyInt());
    }

    // TC14-02: Running duoc sua voi discount hop le
    @Test
    void testUpdateRunningWithValidDiscount() {

        UpdateMarketingStrategyRequest req = request(1);

        mockManager();
        mockLockedStrategy("Running");
        mockValidDiscount();

        when(repository.updateStrategyDetails(
                STRATEGY_ID, req))
                .thenReturn(1);

        when(repository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(
                        response("Running", 1)));

        MarketingStrategyResponse result = service.updateDetails(
                STRATEGY_ID, req, MANAGER_ID);

        assertEquals("Running", result.status());
        assertEquals(1, result.discountId());

        verify(repository).lockDiscountById(1);
        verify(repository).updateStrategyDetails(
                STRATEGY_ID, req);
    }

    // TC14-03: Finished/Cancelled khong duoc sua
    @ParameterizedTest
    @ValueSource(strings = { "Finished", "Cancelled" })
    void testCannotUpdateClosedStrategy(
            String currentStatus) {

        mockManager();
        mockLockedStrategy(currentStatus);

        assertThrows(
                MarketingStrategyManagementService.ManagementConflictException.class,
                () -> service.updateDetails(
                        STRATEGY_ID,
                        request(null),
                        MANAGER_ID));

        verify(repository, never())
                .updateStrategyDetails(anyInt(), any());
    }

    // TC14-04: Admin khong duoc sua
    @Test
    void testUpdateDeniedForAdmin() {

        when(repository.findUserRole(MANAGER_ID))
                .thenReturn(Optional.of("Admin"));

        assertThrows(
                MarketingStrategyManagementService.ManagementForbiddenException.class,
                () -> service.updateDetails(
                        STRATEGY_ID,
                        request(null),
                        MANAGER_ID));

        verify(repository, never())
                .lockStrategyById(anyInt());
    }

    // TC14-05: Khong tim thay Manager
    @Test
    void testManagerNotFound() {

        when(repository.findUserRole(MANAGER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MarketingStrategyManagementService.ManagementForbiddenException.class,
                () -> service.updateDetails(
                        STRATEGY_ID,
                        request(null),
                        MANAGER_ID));

        verify(repository, never())
                .lockStrategyById(anyInt());
    }

    // TC14-06: ID khong hop le
    @Test
    void testInvalidIds() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateDetails(
                        0,
                        request(null),
                        MANAGER_ID));

        assertThrows(
                MarketingStrategyManagementService.ManagementForbiddenException.class,
                () -> service.updateDetails(
                        STRATEGY_ID,
                        request(null),
                        0));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeStrategyStatus(
                        -1,
                        new ChangeMarketingStrategyStatusRequest(
                                "Running"),
                        MANAGER_ID));

        verifyNoInteractions(repository);
    }

    // TC14-07: Du lieu cap nhat khong hop le
    @Test
    void testInvalidUpdateRequestFields() {

        UpdateMarketingStrategyRequest[] invalid = {
                null,

                customRequest(
                        "   ", null, START, END,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        null, 0),

                customRequest(
                        "A".repeat(151), null, START, END,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        null, 0),

                customRequest(
                        "Chien dich", "B".repeat(151),
                        START, END,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        null, 0),

                customRequest(
                        "Chien dich", null,
                        END, START,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        null, 0),

                customRequest(
                        "Chien dich", null,
                        null, END,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        null, 0),

                customRequest(
                        "Chien dich", null,
                        START, END,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        -1, 0),

                customRequest(
                        "Chien dich", null,
                        START, END,
                        BigDecimal.ZERO, BigDecimal.ZERO,
                        null, -1)
        };

        for (UpdateMarketingStrategyRequest req : invalid) {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.updateDetails(
                            STRATEGY_ID, req, MANAGER_ID));
        }

        verifyNoInteractions(repository);
    }

    // TC14-08: Ngan sach/chi phi sai
    @Test
    void testInvalidMoney() {

        UpdateMarketingStrategyRequest[] invalid = {

                customRequest(
                        "A", null, START, END,
                        new BigDecimal("-1"),
                        BigDecimal.ZERO,
                        null, 0),

                customRequest(
                        "A", null, START, END,
                        new BigDecimal("1000000000000.00"),
                        BigDecimal.ZERO,
                        null, 0),

                customRequest(
                        "A", null, START, END,
                        new BigDecimal("100.123"),
                        BigDecimal.ZERO,
                        null, 0),

                customRequest(
                        "A", null, START, END,
                        BigDecimal.ZERO,
                        new BigDecimal("-100"),
                        null, 0),

                customRequest(
                        "A", null, START, END,
                        BigDecimal.ZERO,
                        new BigDecimal("20.123"),
                        null, 0)
        };

        for (UpdateMarketingStrategyRequest req : invalid) {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.updateDetails(
                            STRATEGY_ID, req, MANAGER_ID));
        }

        verifyNoInteractions(repository);
    }

    // TC14-09: Discount khong ton tai
    @Test
    void testDiscountNotFound() {

        mockManager();
        mockLockedStrategy("Planned");

        when(repository.lockDiscountById(1))
                .thenReturn(Optional.empty());

        assertThrows(
                MarketingStrategyManagementService.ManagementDiscountNotFoundException.class,
                () -> service.updateDetails(
                        STRATEGY_ID,
                        request(1),
                        MANAGER_ID));

        verify(repository, never())
                .updateStrategyDetails(anyInt(), any());
    }

    // TC14-10: Discount Disabled
    @Test
    void testDisabledDiscount() {

        mockManager();
        mockLockedStrategy("Running");

        when(repository.lockDiscountById(1))
                .thenReturn(Optional.of(
                        new DiscountSnapshot(
                                1,
                                "Disabled",
                                START,
                                END)));

        assertThrows(
                MarketingStrategyManagementService.ManagementConflictException.class,
                () -> service.updateDetails(
                        STRATEGY_ID,
                        request(1),
                        MANAGER_ID));

        verify(repository, never())
                .updateStrategyDetails(anyInt(), any());
    }

    // TC14-11: Discount khong bao phu thoi gian
    @Test
    void testInvalidDiscountCoverage() {

        mockManager();
        mockLockedStrategy("Planned");

        DiscountSnapshot startsLate = new DiscountSnapshot(
                1,
                "Active",
                START.plusDays(1),
                END);

        DiscountSnapshot endsEarly = new DiscountSnapshot(
                1,
                "Active",
                START,
                END.minusDays(1));

        when(repository.lockDiscountById(1))
                .thenReturn(
                        Optional.of(startsLate),
                        Optional.of(endsEarly));

        for (int i = 0; i < 2; i++) {

            assertThrows(
                    MarketingStrategyManagementService.ManagementConflictException.class,
                    () -> service.updateDetails(
                            STRATEGY_ID,
                            request(1),
                            MANAGER_ID));
        }

        verify(repository, times(2))
                .lockDiscountById(1);

        verify(repository, never())
                .updateStrategyDetails(anyInt(), any());
    }

    // TC14-12: Chien luoc khong ton tai khi sua
    @Test
    void testUpdateStrategyNotFound() {

        mockManager();

        when(repository.lockStrategyById(STRATEGY_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MarketingStrategyManagementService.ManagementStrategyNotFoundException.class,
                () -> service.updateDetails(
                        STRATEGY_ID,
                        request(null),
                        MANAGER_ID));

        verify(repository, never())
                .updateStrategyDetails(anyInt(), any());
    }

    // TC14-13: UPDATE khong anh huong dung 1 dong
    @Test
    void testUpdateAffectedRowsNotOne() {

        UpdateMarketingStrategyRequest req = request(null);

        mockManager();
        mockLockedStrategy("Planned");

        when(repository.updateStrategyDetails(
                STRATEGY_ID, req))
                .thenReturn(0);

        assertThrows(
                MarketingStrategyManagementService.ManagementConflictException.class,
                () -> service.updateDetails(
                        STRATEGY_ID, req, MANAGER_ID));

        verify(repository, never())
                .findById(anyInt());
    }

    // TC14-14: Khong doc duoc sau khi cap nhat
    @Test
    void testUpdateReadBackMissing() {

        UpdateMarketingStrategyRequest req = request(null);

        mockManager();
        mockLockedStrategy("Planned");

        when(repository.updateStrategyDetails(
                STRATEGY_ID, req))
                .thenReturn(1);

        when(repository.findById(STRATEGY_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.updateDetails(
                        STRATEGY_ID, req, MANAGER_ID));
    }

    // =========================================
    // NHOM B - CHUYEN TRANG THAI
    // =========================================

    // TC14-15: Bon chuyen trang thai hop le
    @ParameterizedTest
    @CsvSource({
            "Planned,Running",
            "Planned,Cancelled",
            "Running,Finished",
            "Running,Cancelled"
    })
    void testAllowedStatusTransitions(
            String currentStatus,
            String newStatus) {

        mockManager();
        mockLockedStrategy(currentStatus);

        when(repository.changeStatus(
                STRATEGY_ID,
                currentStatus,
                newStatus)).thenReturn(1);

        when(repository.findById(STRATEGY_ID))
                .thenReturn(Optional.of(
                        response(newStatus, null)));

        MarketingStrategyResponse result = service.changeStrategyStatus(
                STRATEGY_ID,
                new ChangeMarketingStrategyStatusRequest(
                        newStatus),
                MANAGER_ID);

        assertEquals(newStatus, result.status());

        verify(repository).changeStatus(
                STRATEGY_ID,
                currentStatus,
                newStatus);
    }

    // TC14-16: Cac chuyen trang thai bi cam
    @ParameterizedTest
    @CsvSource({
            "Planned,Finished",
            "Planned,Planned",
            "Running,Planned",
            "Running,Running",
            "Finished,Cancelled",
            "Cancelled,Running"
    })
    void testForbiddenStatusTransitions(
            String currentStatus,
            String newStatus) {

        mockManager();
        mockLockedStrategy(currentStatus);

        assertThrows(
                MarketingStrategyManagementService.ManagementConflictException.class,
                () -> service.changeStrategyStatus(
                        STRATEGY_ID,
                        new ChangeMarketingStrategyStatusRequest(
                                newStatus),
                        MANAGER_ID));

        verify(repository, never())
                .changeStatus(anyInt(), any(), any());
    }

    // TC14-17: Trang thai moi khong hop le
    @Test
    void testInvalidNewStatus() {

        String[] invalidStatuses = {
                null,
                "",
                "   ",
                "Pending",
                "running"
        };

        for (String status : invalidStatuses) {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.changeStrategyStatus(
                            STRATEGY_ID,
                            new ChangeMarketingStrategyStatusRequest(
                                    status),
                            MANAGER_ID));
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeStrategyStatus(
                        STRATEGY_ID,
                        null,
                        MANAGER_ID));

        verifyNoInteractions(repository);
    }

    // TC14-18: Chien luoc khong ton tai khi PATCH
    @Test
    void testStatusStrategyNotFound() {

        mockManager();

        when(repository.lockStrategyById(STRATEGY_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MarketingStrategyManagementService.ManagementStrategyNotFoundException.class,
                () -> service.changeStrategyStatus(
                        STRATEGY_ID,
                        new ChangeMarketingStrategyStatusRequest(
                                "Running"),
                        MANAGER_ID));

        verify(repository, never())
                .changeStatus(anyInt(), any(), any());
    }

    // TC14-19: Admin khong duoc chuyen trang thai
    @Test
    void testStatusChangeDeniedForAdmin() {

        when(repository.findUserRole(MANAGER_ID))
                .thenReturn(Optional.of("Admin"));

        assertThrows(
                MarketingStrategyManagementService.ManagementForbiddenException.class,
                () -> service.changeStrategyStatus(
                        STRATEGY_ID,
                        new ChangeMarketingStrategyStatusRequest(
                                "Running"),
                        MANAGER_ID));

        verify(repository, never())
                .lockStrategyById(anyInt());
    }

    // TC14-20: PATCH UPDATE tra ve 0 dong
    @Test
    void testStatusAffectedRowsNotOne() {

        mockManager();
        mockLockedStrategy("Planned");

        when(repository.changeStatus(
                STRATEGY_ID,
                "Planned",
                "Running")).thenReturn(0);

        assertThrows(
                MarketingStrategyManagementService.ManagementConflictException.class,
                () -> service.changeStrategyStatus(
                        STRATEGY_ID,
                        new ChangeMarketingStrategyStatusRequest(
                                "Running"),
                        MANAGER_ID));

        verify(repository, never())
                .findById(anyInt());
    }

    // TC14-21: Khong doc duoc sau khi PATCH
    @Test
    void testStatusReadBackMissing() {

        mockManager();
        mockLockedStrategy("Planned");

        when(repository.changeStatus(
                STRATEGY_ID,
                "Planned",
                "Running")).thenReturn(1);

        when(repository.findById(STRATEGY_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.changeStrategyStatus(
                        STRATEGY_ID,
                        new ChangeMarketingStrategyStatusRequest(
                                "Running"),
                        MANAGER_ID));
    }

    // =========================================
    // NHOM C - LOI DATABASE
    // =========================================

    // TC14-22: Repository loi khi PUT va PATCH
    @Test
    void testDatabaseFailurePropagation() {

        mockManager();
        mockLockedStrategy("Planned");

        UpdateMarketingStrategyRequest req = request(null);

        DataAccessResourceFailureException error = new DataAccessResourceFailureException(
                "Khong ket noi duoc SQL Server");

        when(repository.updateStrategyDetails(
                STRATEGY_ID, req))
                .thenThrow(error);

        DataAccessResourceFailureException putError = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.updateDetails(
                        STRATEGY_ID,
                        req,
                        MANAGER_ID));

        assertSame(error, putError);

        when(repository.changeStatus(
                STRATEGY_ID,
                "Planned",
                "Running")).thenThrow(error);

        DataAccessResourceFailureException patchError = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.changeStrategyStatus(
                        STRATEGY_ID,
                        new ChangeMarketingStrategyStatusRequest(
                                "Running"),
                        MANAGER_ID));

        assertSame(error, patchError);

        verify(repository, never())
                .findById(anyInt());
    }
}
