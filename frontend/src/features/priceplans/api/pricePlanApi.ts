
import { apiClient } from '../../../api/client';

import type {
    CreatePricePlanRequest,
    PricePlanResponse,
} from '../types/PricePlan';

// US-09: Manager tạo kế hoạch điều chỉnh giá
export async function createPricePlan(
    request: CreatePricePlanRequest
): Promise<PricePlanResponse> {
    const response =
        await apiClient.post<PricePlanResponse>(
            '/price-plans',
            request
        );

    return response.data;
}
