
import { apiClient } from '../../../api/client';

import type {
    DiscountEffectivenessResponse,
} from '../types/DiscountEffectiveness';

export async function getDiscountEffectiveness(
    from?: string,
    to?: string,
    signal?: AbortSignal
): Promise<DiscountEffectivenessResponse[]> {
    const response = await apiClient.get<
        DiscountEffectivenessResponse[]
    >('/discounts/effectiveness', {
        params: from && to ? { from, to } : {},
        signal,
    });

    const data = response.data;

    if (
        !Array.isArray(data) ||
        data.some(
            (item) =>
                typeof item.discountId !== 'number' ||
                typeof item.code !== 'string' ||
                typeof item.effectiveStatus !== 'string' ||
                typeof item.uses !== 'number' ||
                !Number.isFinite(Number(item.totalDiscount)) ||
                !Number.isFinite(Number(item.netRevenue)) ||
                (item.usageLimit !== null &&
                    typeof item.usageLimit !== 'number') ||
                (item.remainingUses !== null &&
                    typeof item.remainingUses !== 'number')
        )
    ) {
        throw new Error(
            'Dữ liệu hiệu quả mã chiết khấu không đúng cấu trúc.'
        );
    }

    return data;
}
