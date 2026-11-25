
import { apiClient } from '../../../api/client';

import type {
    MarketingStrategyResponse,
} from '../types/MarketingStrategy';

import type {
    UpdateMarketingStrategyRequest,
    ChangeMarketingStrategyStatusRequest,
} from '../types/MarketingStrategyManagement';

// US-14A: Cập nhật nội dung chiến lược
export async function updateMarketingStrategy(
    strategyId: number,
    request: UpdateMarketingStrategyRequest
): Promise<MarketingStrategyResponse> {
    const response = await apiClient.put<MarketingStrategyResponse>(
        `/marketing-strategies/${strategyId}`,
        request
    );

    return response.data;
}

// US-14B: Chuyển trạng thái chiến lược
export async function changeMarketingStrategyStatus(
    strategyId: number,
    request: ChangeMarketingStrategyStatusRequest
): Promise<MarketingStrategyResponse> {
    const response = await apiClient.patch<MarketingStrategyResponse>(
        `/marketing-strategies/${strategyId}/status`,
        request
    );

    return response.data;
}
