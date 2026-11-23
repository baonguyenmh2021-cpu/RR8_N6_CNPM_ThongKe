
import { apiClient } from '../../../api/client';

import type {
    DiscountEligibilityResponse,
    ExtendDiscountRequest,
    DiscountStateResponse,
} from '../types/DiscountManagement';

// US-07.1: Kiểm tra hiệu lực mã chiết khấu
export async function checkDiscountEligibility(
    discountId: number,
    orderAmount: number,
    signal?: AbortSignal
): Promise<DiscountEligibilityResponse> {
    const response =
        await apiClient.get<DiscountEligibilityResponse>(
            `/discounts/${discountId}/eligibility`,
            {
                params: {
                    orderAmount,
                },
                signal,
            }
        );

    return response.data;
}

// US-07.2: Vô hiệu hóa mã chiết khấu
export async function disableDiscount(
    discountId: number
): Promise<DiscountStateResponse> {
    const response =
        await apiClient.patch<DiscountStateResponse>(
            `/discounts/${discountId}/disable`
        );

    return response.data;
}

// US-07.3: Gia hạn mã chiết khấu
export async function extendDiscount(
    discountId: number,
    request: ExtendDiscountRequest
): Promise<DiscountStateResponse> {
    const response =
        await apiClient.patch<DiscountStateResponse>(
            `/discounts/${discountId}/extend`,
            request
        );

    return response.data;
}
