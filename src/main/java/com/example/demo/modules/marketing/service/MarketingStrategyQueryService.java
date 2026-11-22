
package com.example.demo.modules.marketing.service;

import com.example.demo.modules.marketing.dto.MarketingStrategySummaryResponse;
import com.example.demo.modules.marketing.repository.MarketingStrategyQueryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class MarketingStrategyQueryService {

    private final MarketingStrategyQueryRepository repository;

    // Cac trang thai hop le trong database
    private static final Set<String> VALID_STATUSES = Set.of(
            "Planned",
            "Running",
            "Finished",
            "Cancelled");

    public MarketingStrategyQueryService(
            MarketingStrategyQueryRepository repository) {

        this.repository = repository;
    }

    // US-13: Lay danh sach chien luoc theo trang thai
    @Transactional(readOnly = true)
    public List<MarketingStrategySummaryResponse> getStrategies(
            String status) {

        // 1. Khong truyen status -> lay tat ca
        if (status == null) {

            return repository.findStrategies(null);
        }

        // 2. Loai bo khoang trang dau/cuoi
        String normalizedStatus = status.trim();

        // 3. Kiem tra status co hop le khong
        if (!VALID_STATUSES.contains(normalizedStatus)) {

            throw new IllegalArgumentException(
                    "Trang thai khong hop le. "
                            + "Chi chap nhan: Planned, Running, "
                            + "Finished, Cancelled");
        }

        // 4. Lay danh sach theo status
        return repository.findStrategies(normalizedStatus);
    }
}
