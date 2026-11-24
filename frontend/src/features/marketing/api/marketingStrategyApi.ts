
import { apiClient } from '../../../api/client';

import type {
    CreateMarketingStrategyRequest,
    MarketingStrategyResponse,
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
