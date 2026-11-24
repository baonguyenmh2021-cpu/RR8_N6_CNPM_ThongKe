
import { apiClient } from '../../../api/client';

import type {
    ReviewPricePlanRequest,
    ReviewPricePlanResponse,
} from '../types/PricePlanReview';

// US-10: Admin phê duyệt hoặc từ chối kế hoạch giá
export async function reviewPricePlan(
    planId: number,
    request: ReviewPricePlanRequest
): Promise<ReviewPricePlanResponse> {
    const response = await apiClient.patch<ReviewPricePlanResponse>(
        `/price-plans/${planId}/review`,
        request
    );

    return response.data;
}
