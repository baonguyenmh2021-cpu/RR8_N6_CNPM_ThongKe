
package com.example.demo.modules.marketing.service;

import com.example.demo.modules.marketing.dto.MarketingStrategySummaryResponse;
import com.example.demo.modules.marketing.repository.MarketingStrategyQueryRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketingUS13Test {

    @Mock
    private MarketingStrategyQueryRepository repository;

    @InjectMocks
    private MarketingStrategyQueryService service;

    // Tao du lieu chien luoc gia lap
    private MarketingStrategySummaryResponse strategy(
            int id,
            String title,
            String status) {

        return new MarketingStrategySummaryResponse(
                id,
                title,
                "Sinh vien",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("5000000.00"),
                status);
    }

    // TC13-01: Khong truyen status
    @Test
    void testGetAllStrategies() {

        List<MarketingStrategySummaryResponse> data = List.of(
                strategy(1, "Chien dich A", "Running"),
                strategy(2, "Chien dich B", "Planned"),
                strategy(3, "Chien dich C", "Finished"));

        when(repository.findStrategies(null))
                .thenReturn(data);

        List<MarketingStrategySummaryResponse> result = service.getStrategies(null);

        assertEquals(3, result.size());

        assertSame(data, result);

        verify(repository).findStrategies(null);
        verifyNoMoreInteractions(repository);
    }

    // TC13-02: Loc Planned
    @Test
    void testFilterPlanned() {

        List<MarketingStrategySummaryResponse> data = List.of(
                strategy(2, "Thu hut sinh vien", "Planned"));

        when(repository.findStrategies("Planned"))
                .thenReturn(data);

        List<MarketingStrategySummaryResponse> result = service.getStrategies("Planned");

        assertEquals(1, result.size());
        assertEquals("Planned", result.get(0).status());
        assertEquals(2, result.get(0).strategyId());

        verify(repository).findStrategies("Planned");
    }

    // TC13-03: Loc Running
    @Test
    void testFilterRunning() {

        List<MarketingStrategySummaryResponse> data = List.of(
                strategy(1, "Uu dai mua thu", "Running"));

        when(repository.findStrategies("Running"))
                .thenReturn(data);

        List<MarketingStrategySummaryResponse> result = service.getStrategies("Running");

        assertEquals(1, result.size());
        assertEquals("Running", result.get(0).status());

        verify(repository).findStrategies("Running");
    }

    // TC13-04: Loc Finished
    @Test
    void testFilterFinished() {

        List<MarketingStrategySummaryResponse> data = List.of(
                strategy(3, "Chien dich mua he", "Finished"));

        when(repository.findStrategies("Finished"))
                .thenReturn(data);

        List<MarketingStrategySummaryResponse> result = service.getStrategies("Finished");

        assertEquals(1, result.size());
        assertEquals("Finished", result.get(0).status());

        verify(repository).findStrategies("Finished");
    }

    // TC13-05: Loc Cancelled
    @Test
    void testFilterCancelled() {

        List<MarketingStrategySummaryResponse> data = List.of(
                strategy(4, "Chien dich da huy", "Cancelled"));

        when(repository.findStrategies("Cancelled"))
                .thenReturn(data);

        List<MarketingStrategySummaryResponse> result = service.getStrategies("Cancelled");

        assertEquals(1, result.size());
        assertEquals("Cancelled", result.get(0).status());

        verify(repository).findStrategies("Cancelled");
    }

    // TC13-06: Tu dong loai khoang trang
    @Test
    void testTrimStatus() {

        when(repository.findStrategies("Running"))
                .thenReturn(List.of(
                        strategy(1, "Chien dich A", "Running")));

        List<MarketingStrategySummaryResponse> result = service.getStrategies("  Running  ");

        assertEquals(1, result.size());

        verify(repository).findStrategies("Running");

        verify(repository, never())
                .findStrategies("  Running  ");
    }

    // TC13-07: Trang thai hop le nhung khong co du lieu
    @Test
    void testEmptyResult() {

        when(repository.findStrategies("Finished"))
                .thenReturn(List.of());

        List<MarketingStrategySummaryResponse> result = service.getStrategies("Finished");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(repository).findStrategies("Finished");
    }

    // TC13-08: Status la chuoi rong
    @Test
    void testEmptyStatus() {

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.getStrategies(""));

        assertTrue(
                error.getMessage().contains(
                        "Trang thai khong hop le"));

        verifyNoInteractions(repository);
    }

    // TC13-09: Status chi co khoang trang
    @Test
    void testWhitespaceStatus() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getStrategies("   "));

        verifyNoInteractions(repository);
    }

    // TC13-10: Trang thai khong ton tai
    @Test
    void testInvalidStatus() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getStrategies("Pending"));

        verifyNoInteractions(repository);
    }

    // TC13-11: Sai chu hoa/chu thuong
    @Test
    void testWrongCaseStatus() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getStrategies("running"));

        verifyNoInteractions(repository);
    }

    // TC13-12: Chuoi "null" khong phai null
    @Test
    void testStringNullStatus() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getStrategies("null"));

        verifyNoInteractions(repository);
    }

    // TC13-13: Repository gap loi database
    @Test
    void testDatabaseFailure() {

        DataAccessResourceFailureException dbError = new DataAccessResourceFailureException(
                "Khong ket noi duoc SQL Server");

        when(repository.findStrategies(null))
                .thenThrow(dbError);

        DataAccessResourceFailureException actual = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.getStrategies(null));

        assertSame(dbError, actual);

        verify(repository).findStrategies(null);
    }
}
