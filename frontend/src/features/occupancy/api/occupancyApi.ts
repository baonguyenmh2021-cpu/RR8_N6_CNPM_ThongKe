
import { apiClient } from '../../../api/client';

import type {
    OccupancyCategoryResponse,
} from '../types/Occupancy';

// US-08: Thống kê tỷ lệ lấp đầy theo loại phòng
export async function getOccupancyCategories(
    from: string,
    to: string,
    signal?: AbortSignal
): Promise<OccupancyCategoryResponse[]> {

    const response =
        await apiClient.get<OccupancyCategoryResponse[]>(
            '/occupancy/categories',
            {
                params: {
                    from,
                    to,
                },
                signal,
            }
        );

    return response.data;
}
