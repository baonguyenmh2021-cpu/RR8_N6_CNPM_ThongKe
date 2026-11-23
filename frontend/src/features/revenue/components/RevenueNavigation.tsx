
import { Button, Group } from '@mantine/core';
import { Link, useLocation } from 'react-router-dom';

export default function RevenueNavigation() {
    const { pathname } = useLocation();

    const items = [
        {
            label: 'Theo tháng',
            path: '/revenue',
        },
        {
            label: 'Theo năm',
            path: '/revenue/yearly',
        },
    ];

    return (
        <Group gap="xs">
            {items.map((item) => {
                const isActive = pathname === item.path;

                return (
                    <Button
                        key={item.path}
                        component={Link}
                        to={item.path}
                        size="sm"
                        radius="sm"
                        variant={isActive ? 'light' : 'subtle'}
                    >
                        {item.label}
                    </Button>
                );
            })}
        </Group>
    );
}
