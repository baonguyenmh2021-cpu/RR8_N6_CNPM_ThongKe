
import type { ApiNumber } from './MonthlyRevenue';

export interface MonthlyRevenueItem {
    month: number;
    revenue: ApiNumber;
}

export interface YearlyRevenueResponse {
    year: number;
    totalRevenue: ApiNumber;
    months: MonthlyRevenueItem[];
    highestRevenueMonths: number[];
}
