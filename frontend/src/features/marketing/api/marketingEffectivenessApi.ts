
import { apiClient } from '../../../api/client';

import type {
    MarketingEffectivenessResponse,
} from '../types/MarketingEffectiveness';

// US-12: Tra cứu hiệu quả chiến lược Marketing
export async function getMarketingEffectiveness(
    strategyId: number,
    signal?: AbortSignal
): Promise<MarketingEffectivenessResponse> {

    if (
        !Number.isSafeInteger(strategyId) ||
        strategyId <= 0
    ) {
        throw new Error('Strategy ID không hợp lệ.');
    }

    const response =
        await apiClient.get<MarketingEffectivenessResponse>(
            `/marketing-strategies/${strategyId}/effectiveness`,
            { signal }
        );

    return response.data;
}
