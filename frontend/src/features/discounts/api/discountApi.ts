
import { apiClient } from '../../../api/client';

import type {
    CreateDiscountRequest,
    DiscountResponse,
} from '../types/Discount';

// US-05: Tạo mã giảm giá
export async function createDiscount(
    request: CreateDiscountRequest
): Promise<DiscountResponse> {
    const response = await apiClient.post<DiscountResponse>(
        '/discounts',
        request
    );

    return response.data;
}
