
import {
    AppShell,
    NavLink,
    Stack,
    Text,
} from '@mantine/core';

import { Link, useLocation } from 'react-router-dom';
import { navigationItems } from '../../app/navigation';

import styles from './Sidebar.module.css';

type SidebarProps = {
    onNavigate: () => void;
};

export default function Sidebar({
    onNavigate,
}: SidebarProps) {
    const { pathname } = useLocation();

    return (
        <AppShell.Navbar p="md" className={styles.navbar}>
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
            </AppShell.Section>

            <AppShell.Section grow>
                <Stack gap={4}>
                    {navigationItems.map((item) => {
                        const Icon = item.icon;
                        const active = pathname === item.path;

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
                <Text size="xs" c="dimmed" ta="center">
                    Home Rent · RR8
                </Text>
            </AppShell.Section>
        </AppShell.Navbar>
    );
}
