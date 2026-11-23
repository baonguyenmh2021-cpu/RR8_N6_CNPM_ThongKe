
import {
    BrowserRouter,
    Navigate,
    Route,
    Routes,
} from 'react-router-dom';

import DashboardLayout from '../layouts/DashboardLayout';
import RevenuePage from '../features/revenue/pages/RevenuePage';
import ComingSoonPage from '../pages/ComingSoonPage';
import YearlyRevenuePage from '../features/revenue/pages/YearlyRevenuePage';
import CategoryRevenuePage from '../features/revenue/pages/CategoryRevenuePage';
import ClosedRevenuePage from '../features/revenue/pages/ClosedRevenuePage';
import CreateDiscountPage from '../features/discounts/pages/CreateDiscountPage';
import DiscountEffectivenessPage from '../features/discounts/pages/DiscountEffectivenessPage';

export default function AppRouter() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<DashboardLayout />}>
                    <Route
                        index
                        element={<Navigate to="/revenue" replace />}
                    />

                    <Route
                        path="revenue"
                        element={<RevenuePage />}
                    />

                    <Route
                        path="revenue/yearly"
                        element={<YearlyRevenuePage />}
                    />

                    {/* US-03: Doanh thu theo loại phòng */}
                    <Route
                        path="revenue/categories"
                        element={<CategoryRevenuePage />}
                    />

                    {/* US-04: Xem báo cáo doanh thu đã chốt */}
                    <Route
                        path="revenue/closed"
                        element={<ClosedRevenuePage />}
                    />
                    {/* US-05: Tạo mã chiết khấu */}
                    <Route
                        path="discounts"
                        element={<CreateDiscountPage />}
                    />
                    
                    {/* US-06: Hiệu quả mã chiết khấu */}
                    <Route
                        path="discounts/effectiveness"
                        element={<DiscountEffectivenessPage />}
                    />
                    <Route
                        path="occupancy"
                        element={
                            <ComingSoonPage title="Tỷ lệ lấp đầy" />
                        }
                    />

                    <Route
                        path="price-plans"
                        element={
                            <ComingSoonPage title="Kế hoạch giá" />
                        }
                    />

                    <Route
                        path="marketing"
                        element={
                            <ComingSoonPage title="Chiến lược marketing" />
                        }
                    />

                    <Route
                        path="*"
                        element={
                            <ComingSoonPage
                                title="Không tìm thấy trang"
                                description="Đường dẫn này không tồn tại trong RR8."
                            />
                        }
                    />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}
