
import { apiClient } from '../../../api/client';

import type {
    ClosedMonthlyReportResponse,
} from '../types/ClosedMonthlyReport';

// US-04: Chốt báo cáo trong môi trường local-demo
export async function closeMonthlyReport(
    year: number,
    month: number
): Promise<ClosedMonthlyReportResponse> {
    const response =
        await apiClient.post<ClosedMonthlyReportResponse>(
            '/revenue/closed',
            null,
            {
                params: { year, month },
                headers: {
                    'X-RR8-Demo-Only': 'confirm-close',
                },
                timeout: 20000,
            }
        );

    return response.data;
}
