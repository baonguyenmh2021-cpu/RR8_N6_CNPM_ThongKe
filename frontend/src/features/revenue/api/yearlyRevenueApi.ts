
import { apiClient } from '../../../api/client';
import type { YearlyRevenueResponse } from '../types/YearlyRevenue';

export async function getYearlyRevenue(
    year: number,
    signal?: AbortSignal
): Promise<YearlyRevenueResponse> {
    const response = await apiClient.get<YearlyRevenueResponse>(
        '/revenue/yearly',
        {
            params: { year },
            signal,
        }
    );

    const data = response.data;

    if (
        !data ||
        !Array.isArray(data.months) ||
        !Array.isArray(data.highestRevenueMonths) ||
        data.months.length !== 12 ||
        !Number.isFinite(Number(data.totalRevenue)) ||
        data.months.some(
            (item) =>
                !Number.isInteger(item.month) ||
                item.month < 1 ||
                item.month > 12 ||
                !Number.isFinite(Number(item.revenue))
        ) ||
        new Set(data.months.map((item) => item.month)).size !== 12
    ) {
        throw new Error(
            'Dữ liệu doanh thu năm không đúng cấu trúc dự kiến.'
        );
    }

    return data;
}
