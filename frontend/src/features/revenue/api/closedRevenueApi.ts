
import { apiClient } from '../../../api/client';

import type {
  ClosedMonthlyReportResponse,
} from '../types/ClosedMonthlyReport';

// US-04: Đọc báo cáo doanh thu đã chốt
export async function getClosedMonthlyReport(
  year: number,
  month: number,
  signal?: AbortSignal
): Promise<ClosedMonthlyReportResponse> {
  const response =
    await apiClient.get<ClosedMonthlyReportResponse>(
      '/revenue/closed',
      {
        params: {
          year,
          month,
        },
        signal,
      }
    );

  const data = response.data;

  if (
    !data ||
    typeof data.reportId !== 'number' ||
    typeof data.year !== 'number' ||
    typeof data.month !== 'number' ||
    !Number.isFinite(Number(data.totalRevenue)) ||
    !Number.isFinite(Number(data.totalDiscount)) ||
    typeof data.bookingCount !== 'number' ||
    typeof data.cancelledBookings !== 'number'
  ) {
    throw new Error(
      'Dữ liệu báo cáo đã chốt không khớp DTO dự kiến.'
    );
  }

  return data;
}