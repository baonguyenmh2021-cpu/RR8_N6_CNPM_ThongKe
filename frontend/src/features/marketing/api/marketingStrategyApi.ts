
import { apiClient } from '../../../api/client';

import type {
    CreateMarketingStrategyRequest,
    MarketingStrategyResponse,
    MarketingStrategySummaryResponse,
    MarketingStrategyStatus,
} from '../types/MarketingStrategy';

// US-11: Manager tạo chiến lược Marketing
export async function createMarketingStrategy(
    request: CreateMarketingStrategyRequest
): Promise<MarketingStrategyResponse> {

    const response =
        await apiClient.post<MarketingStrategyResponse>(
            '/marketing-strategies',
            request
        );

    return response.data;
}

// US-13: Lấy danh sách chiến lược Marketing
export async function getMarketingStrategies(
    status?: MarketingStrategyStatus,
    signal?: AbortSignal
): Promise<MarketingStrategySummaryResponse[]> {

    const response =
        await apiClient.get<MarketingStrategySummaryResponse[]>(
            '/marketing-strategies',
            {
                params: status ? { status } : {},
                signal,
            }
        );

    return response.data;
}
