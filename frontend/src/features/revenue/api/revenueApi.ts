
import { apiClient } from '../../../api/client';

import type {
    MonthlyRevenueResponse,
} from '../types/MonthlyRevenue';

export async function getMonthlyRevenue(
    year: number,
    month: number,
    signal?: AbortSignal
): Promise<MonthlyRevenueResponse> {
    const response =
        await apiClient.get<MonthlyRevenueResponse>(
            '/revenue/monthly',
            {
                params: { year, month },
                signal,
            }
        );

    const data = response.data;

    if (
        !data ||
        typeof data !== 'object' ||
        !('revenue' in data) ||
        !('bookingCount' in data) ||
        !('totalDiscount' in data) ||
        !('cancelledBookingsCreatedInMonth' in data) ||
        !('previousMonthRevenue' in data) ||
        !('revenueChangePercent' in data)
    ) {
        throw new Error(
            'Response API không khớp DTO US-01 đã dự kiến.'
        );
    }

    return data;
}
