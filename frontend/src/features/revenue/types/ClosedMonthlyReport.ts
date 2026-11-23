
import type { ApiNumber } from './MonthlyRevenue';

export interface ClosedMonthlyReportResponse {
    reportId: number;
    year: number;
    month: number;

    totalRevenue: ApiNumber;
    totalDiscount: ApiNumber;

    bookingCount: number;
    cancelledBookings: number;

    generatedBy: number;
    generatedAt: string;
}