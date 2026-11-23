
import type { LucideIcon } from 'lucide-react';
import {
    ChartNoAxesColumn,
    Tags,
    BedDouble,
    ClipboardList,
    Megaphone,
} from 'lucide-react';

export type NavigationItem = {
    label: string;
    path: string;
    icon: LucideIcon;
};

export const navigationItems: NavigationItem[] = [
    {
        label: 'Doanh thu',
        path: '/revenue',
        icon: ChartNoAxesColumn,
    },
    {
        label: 'Mã chiết khấu',
        path: '/discounts',
        icon: Tags,
    },
    {
        label: 'Tỷ lệ lấp đầy',
        path: '/occupancy',
        icon: BedDouble,
    },
    {
        label: 'Kế hoạch giá',
        path: '/price-plans',
        icon: ClipboardList,
    },
    {
        label: 'Chiến lược marketing',
        path: '/marketing',
        icon: Megaphone,
    },
];
