
import {
    BrowserRouter,
    Navigate,
    Route,
    Routes,
} from 'react-router-dom';

import DashboardLayout from '../layouts/DashboardLayout';

import RevenuePage from '../features/revenue/pages/RevenuePage';
import YearlyRevenuePage from '../features/revenue/pages/YearlyRevenuePage';
import CategoryRevenuePage from '../features/revenue/pages/CategoryRevenuePage';
import ClosedRevenuePage from '../features/revenue/pages/ClosedRevenuePage';

import CreateDiscountPage from '../features/discounts/pages/CreateDiscountPage';
import DiscountEffectivenessPage from '../features/discounts/pages/DiscountEffectivenessPage';
import ManageDiscountPage from '../features/discounts/pages/ManageDiscountPage';

import OccupancyPage from '../features/occupancy/pages/OccupancyPage';

import ComingSoonPage from '../pages/ComingSoonPage';

import { DemoRoleProvider } from '../features/auth/context/DemoRoleContext';

import DemoRoleGuard from '../features/auth/guards/DemoRoleGuard';

import CreatePricePlanPage from '../features/priceplans/pages/CreatePricePlanPage';

import PricePlanRoute from '../features/priceplans/pages/PricePlanRoute';

import CreateMarketingStrategyPage from '../features/marketing/pages/CreateMarketingStrategyPage';

import MarketingEffectivenessPage from '../features/marketing/pages/MarketingEffectivenessPage';

export default function AppRouter() {
    return (
        <DemoRoleProvider>
            <BrowserRouter>
                <Routes>
                    <Route
                        path="/"
                        element={<DashboardLayout />}
                    >
                        <Route
                            index
                            element={
                                <Navigate
                                    to="/revenue"
                                    replace
                                />
                            }
                        />

                        {/* US-01: Doanh thu theo tháng */}
                        <Route
                            path="revenue"
                            element={<RevenuePage />}
                        />

                        {/* US-02: Doanh thu theo năm */}
                        <Route
                            path="revenue/yearly"
                            element={<YearlyRevenuePage />}
                        />

                        {/* US-03: Doanh thu theo loại phòng */}
                        <Route
                            path="revenue/categories"
                            element={<CategoryRevenuePage />}
                        />

                        {/* US-04: Chốt báo cáo - Admin */}

                        {/* US-04: Xem và chốt báo cáo doanh thu */}
                        <Route
                            path="revenue/closed"
                            element={
                                <DemoRoleGuard
                                    allowedRoles={['MANAGER', 'ADMIN']}
                                >
                                    <ClosedRevenuePage />
                                </DemoRoleGuard>
                            }
                        />


                        {/* US-05: Tạo mã chiết khấu */}
                        <Route
                            path="discounts"
                            element={
                                <DemoRoleGuard
                                    allowedRoles={['MANAGER']}
                                >
                                    <CreateDiscountPage />
                                </DemoRoleGuard>
                            }
                        />

                        {/* US-06: Hiệu quả mã chiết khấu */}
                        <Route
                            path="discounts/effectiveness"
                            element={
                                <DemoRoleGuard
                                    allowedRoles={['MANAGER']}
                                >
                                    <DiscountEffectivenessPage />
                                </DemoRoleGuard>
                            }
                        />

                        {/* US-07: Quản lý mã chiết khấu */}
                        <Route
                            path="discounts/manage"
                            element={
                                <DemoRoleGuard
                                    allowedRoles={['MANAGER']}
                                >
                                    <ManageDiscountPage />
                                </DemoRoleGuard>
                            }
                        />

                        {/* US-08: Tỷ lệ lấp đầy */}
                        <Route
                            path="occupancy"
                            element={<OccupancyPage />}
                        />

                        {/* US-09 và US-10: Kế hoạch điều chỉnh giá */}
                        <Route
                            path="price-plans"
                            element={<PricePlanRoute />}
                        />

                        {/* US-11: Manager tạo chiến lược Marketing */}
                        <Route
                            path="marketing"
                            element={
                                <DemoRoleGuard allowedRoles={['MANAGER']}>
                                    <CreateMarketingStrategyPage />
                                </DemoRoleGuard>
                            }
                        />

                        {/* US-12: Xem hiệu quả chiến lược Marketing */}
                        <Route
                            path="marketing/effectiveness"
                            element={
                                <DemoRoleGuard allowedRoles={['MANAGER']}>
                                    <MarketingEffectivenessPage />
                                </DemoRoleGuard>
                            }
                        />

                        {/* Không tìm thấy trang */}
                        <Route
                            path="*"
                            element={
                                <ComingSoonPage
                                    title="Không tìm thấy trang"
                                    description="Đường dẫn không tồn tại trong hệ thống."
                                />
                            }
                        />
                    </Route>
                </Routes>
            </BrowserRouter>
        </DemoRoleProvider>
    );
}
