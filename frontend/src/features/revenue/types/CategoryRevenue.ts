
import type { ApiNumber } from './MonthlyRevenue';

// Doanh thu của một loại phòng
export interface CategoryRevenueItem {
    categoryId: number;
    categoryName: string;
    revenue: ApiNumber;
    bookingCount: number;
}

// Kết quả thống kê US-03
export interface CategoryRevenueResponse {
    year: number;
    month: number;
    totalRevenue: ApiNumber;
    categories: CategoryRevenueItem[];
}
