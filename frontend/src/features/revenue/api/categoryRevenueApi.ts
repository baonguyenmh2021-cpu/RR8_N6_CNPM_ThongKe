
import { apiClient } from '../../../api/client';

import type {
    CategoryRevenueResponse,
} from '../types/CategoryRevenue';

// US-03: Lấy doanh thu theo loại phòng
export async function getCategoryRevenue(
    year: number,
    month: number,
    signal?: AbortSignal
): Promise<CategoryRevenueResponse> {
    const response =
        await apiClient.get<CategoryRevenueResponse>(
            '/revenue/categories',
            {
                params: {
                    year,
                    month,
                },
                signal,
            }
        );

    const data = response.data;

    // Kiểm tra cấu trúc JSON backend
    if (
        !data ||
        !Array.isArray(data.categories) ||
        !Number.isFinite(Number(data.totalRevenue)) ||
        data.categories.some(
            (item) =>
                typeof item.categoryId !== 'number' ||
                typeof item.categoryName !== 'string' ||
                !Number.isFinite(Number(item.revenue)) ||
                typeof item.bookingCount !== 'number'
        )
    ) {
        throw new Error(
            'Dữ liệu doanh thu theo loại phòng không hợp lệ.'
        );
    }

    return data;
}
