
import {
    AppShell,
    Badge,
    Box,
    NavLink,
    Stack,
    Text,
} from '@mantine/core';

import { Link, useLocation } from 'react-router-dom';

import { navigationItems } from '../../app/navigation';

import { useDemoRole } from '../../features/auth/hooks/useDemoRole';

import { DEMO_ROLE_LABELS } from '../../features/auth/types/DemoRole';

import styles from './Sidebar.module.css';

type SidebarProps = {
    onNavigate: () => void;
};

export default function Sidebar({
    onNavigate,
}: SidebarProps) {
    const { pathname } = useLocation();

    const { role } = useDemoRole();

    const visibleItems = navigationItems.filter(
        (item) => item.roles.includes(role)
    );

    return (
        <AppShell.Navbar
            p="md"
            className={styles.navbar}
        >
            <AppShell.Section>
                <Text
                    size="xs"
                    fw={600}
                    c="dimmed"
                    tt="uppercase"
                    px="sm"
                    mb="md"
                >
                    Quản lý thống kê
                </Text>

                <Box px="sm" mb="md">
                    <Badge
                        color={role === 'ADMIN' ? 'teal' : 'blue'}
                        variant="light"
                        size="lg"
                        fullWidth
                    >
                        {DEMO_ROLE_LABELS[role]}
                    </Badge>
                </Box>
            </AppShell.Section>

            <AppShell.Section grow>
                <Stack gap={4}>
                    {visibleItems.map((item) => {
                        const Icon = item.icon;

                        const active =
                            pathname === item.path ||
                            pathname.startsWith(`${item.path}/`);

                        return (
                            <NavLink
                                key={item.path}
                                component={Link}
                                to={item.path}
                                label={item.label}
                                leftSection={<Icon size={18} />}
                                active={active}
                                onClick={onNavigate}
                                className={styles.menuItem}
                            />
                        );
                    })}
                </Stack>
            </AppShell.Section>

            <AppShell.Section>
                <Text
                    size="xs"
                    c="dimmed"
                    ta="center"
                >
                    Home Rent · RR8
                </Text>
            </AppShell.Section>
        </AppShell.Navbar>
    );
}
