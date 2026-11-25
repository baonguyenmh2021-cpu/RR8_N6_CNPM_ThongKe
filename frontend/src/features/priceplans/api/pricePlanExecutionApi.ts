
import { apiClient } from '../../../api/client';

import type {
    PricePlanExecutionResponse,
} from '../types/PricePlanExecution';

// US-15A: Approved -> Applied
export async function applyPricePlan(
    planId: number
): Promise<PricePlanExecutionResponse> {
    const response =
        await apiClient.post<PricePlanExecutionResponse>(
            `/price-plans/${planId}/apply`
        );

    return response.data;
}

// US-15B: Applied -> Ended
export async function endPricePlan(
    planId: number
): Promise<PricePlanExecutionResponse> {
    const response =
        await apiClient.post<PricePlanExecutionResponse>(
            `/price-plans/${planId}/end`
        );

    return response.data;
}
