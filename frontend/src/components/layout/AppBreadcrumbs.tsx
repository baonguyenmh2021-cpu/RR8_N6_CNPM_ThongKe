
import {
    Anchor,
    Breadcrumbs,
    Text,
} from '@mantine/core';

import { Link, useLocation } from 'react-router-dom';
import { navigationItems } from '../../app/navigation';

export default function AppBreadcrumbs() {
    const { pathname } = useLocation();

    const currentPage = navigationItems.find(
        (item) => item.path === pathname
    );

    return (
        <Breadcrumbs separator="/">
            <Anchor component={Link} to="/revenue" size="sm">
                Trang chủ
            </Anchor>

            <Text size="sm" c="dimmed">
                {currentPage?.label ?? 'Không tìm thấy trang'}
            </Text>
        </Breadcrumbs>
    );
}
