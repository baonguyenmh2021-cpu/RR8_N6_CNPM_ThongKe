
import { Anchor, Breadcrumbs, Text } from '@mantine/core';
import { Link, useLocation } from 'react-router-dom';
import { navigationItems } from '../../app/navigation';

export default function AppBreadcrumbs() {
    const { pathname } = useLocation();

    const currentPage = navigationItems.find(
        (item) =>
            pathname === item.path ||
            pathname.startsWith(`${item.path}/`)
    );

    const isYearly = pathname === '/revenue/yearly';

    return (
        <Breadcrumbs separator="/">
            <Anchor component={Link} to="/revenue" size="sm">
                Trang chủ
            </Anchor>

            {isYearly ? (
                <>
                    <Anchor component={Link} to="/revenue" size="sm">
                        Doanh thu
                    </Anchor>

                    {/* <Text size="sm" c="dimmed">
                        Theo năm
                    </Text> */}
                </>
            ) : (
                <Text size="sm" c="dimmed">
                    {currentPage?.label ?? 'Không tìm thấy trang'}
                </Text>
            )}
        </Breadcrumbs>
    );
}
