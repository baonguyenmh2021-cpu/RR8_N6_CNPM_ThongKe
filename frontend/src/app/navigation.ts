
import type { LucideIcon } from 'lucide-react';

import {
    ChartNoAxesColumn,
    Tags,
    BedDouble,
    ClipboardList,
    Megaphone,
} from 'lucide-react';

import type { DemoRole } from '../features/auth/types/DemoRole';

export type NavigationItem = {
    label: string;
    path: string;
    icon: LucideIcon;
    roles: DemoRole[];
};

export const navigationItems: NavigationItem[] = [
    {
        label: 'Doanh thu',
        path: '/revenue',
        icon: ChartNoAxesColumn,
        roles: ['MANAGER', 'ADMIN'],
    },
    {
        label: 'Mã chiết khấu',
        path: '/discounts',
        icon: Tags,
        roles: ['MANAGER'],
    },
    {
        label: 'Tỷ lệ lấp đầy',
        path: '/occupancy',
        icon: BedDouble,
        roles: ['MANAGER', 'ADMIN'],
    },
    {
        label: 'Kế hoạch giá',
        path: '/price-plans',
        icon: ClipboardList,
        roles: ['MANAGER', 'ADMIN'],
    },
    {
        label: 'Chiến lược marketing',
        path: '/marketing',
        icon: Megaphone,
        roles: ['MANAGER'],
    },
];
