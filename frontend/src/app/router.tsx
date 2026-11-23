
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

                    <Route
                        path="discounts"
                        element={
                            <ComingSoonPage title="Mã chiết khấu" />
                        }
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
