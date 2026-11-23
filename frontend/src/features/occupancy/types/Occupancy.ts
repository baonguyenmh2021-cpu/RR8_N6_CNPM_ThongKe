
export interface OccupancyCategoryResponse {
    categoryId: number;

    categoryName: string;

    // Số phòng không ở trạng thái Maintenance
    availableRooms: number;

    // Tổng số đêm trong khoảng thống kê
    periodNights: number;

    // Tổng số đêm đã được đặt
    bookedNights: number;

    // Tỷ lệ lấp đầy (%)
    // Null khi không có phòng khả dụng
    occupancyRatePercent: number | string | null;

    // Tổng doanh thu từ payment Paid
    paidRevenue: number | string;

    // Giá phòng hiện tại thấp nhất
    // Null khi không có phòng khả dụng
    minCurrentPrice: number | string | null;

    // Giá phòng hiện tại cao nhất
    // Null khi không có phòng khả dụng
    maxCurrentPrice: number | string | null;
}

export interface OccupancyFilter {
    from: string;
    to: string;
}
