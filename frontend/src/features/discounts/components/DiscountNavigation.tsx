
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
        {
            label: 'Quản lý mã chiết khấu',
            path: '/discounts/manage',
        },
    ];

    return (
        <Group
            grow
            gap="sm"
            wrap="nowrap"
            style={{
                width: '100%',
                maxWidth: 780,
                marginTop: 12,
                marginBottom: 24,
            }}
        >
            {items.map((item) => {
                const active = pathname === item.path;

                return (
                    <Button
                        key={item.path}
                        component={Link}
                        to={item.path}
                        fullWidth
                        h={42}
                        px="sm"
                        size="sm"
                        radius="sm"
                        variant={active ? 'light' : 'subtle'}
                        style={{
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {item.label}
                    </Button>
                );
            })}
        </Group>
    );
}
