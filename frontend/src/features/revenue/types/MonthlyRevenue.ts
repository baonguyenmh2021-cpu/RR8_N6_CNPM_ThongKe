
export type ApiNumber = number | string;

export interface MonthlyRevenueResponse {
    revenue: ApiNumber;
    bookingCount: number;
    totalDiscount: ApiNumber;
    cancelledBookingsCreatedInMonth: number;
    previousMonthRevenue: ApiNumber;
    revenueChangePercent: ApiNumber | null;
}
