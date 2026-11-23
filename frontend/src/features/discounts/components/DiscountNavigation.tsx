
import { Button, Group } from '@mantine/core';
import { Link, useLocation } from 'react-router-dom';

export default function DiscountNavigation() {
    const { pathname } = useLocation();

    const items = [
        {
            label: 'Tạo mã chiết khấu',
            path: '/discounts',
        },
        {
            label: 'Hiệu quả mã chiết khấu',
            path: '/discounts/effectiveness',
        },
    ];

    return (
        <Group gap="xs" wrap="wrap">
            {items.map((item) => {
                const active = pathname === item.path;

                return (
                    <Button
                        key={item.path}
                        component={Link}
                        to={item.path}
                        size="sm"
                        radius="sm"
                        variant={active ? 'light' : 'subtle'}
                    >
                        {item.label}
                    </Button>
                );
            })}
        </Group>
    );
}
