
import {
    ActionIcon,
    AppShell,
    Burger,
    Group,
    SegmentedControl,
    Text,
    useMantineColorScheme,
} from '@mantine/core';

import { Moon, Sun } from 'lucide-react';

import { useDemoRole } from '../../features/auth/hooks/useDemoRole';

import styles from './Header.module.css';

import type { DemoRole } from '../../features/auth/types/DemoRole';

type HeaderProps = {
    opened: boolean;
    onToggleMenu: () => void;
};

export default function Header({
    opened,
    onToggleMenu,
}: HeaderProps) {
    const { colorScheme, toggleColorScheme } =
        useMantineColorScheme();

    const { role, setRole } = useDemoRole();

    const isDark = colorScheme === 'dark';

    return (
        <AppShell.Header className={styles.header}>
            <Group
                h="100%"
                px="lg"
                justify="space-between"
                wrap="nowrap"
            >
                <Group gap="md" wrap="nowrap">
                    <Burger
                        opened={opened}
                        onClick={onToggleMenu}
                        hiddenFrom="sm"
                        size="sm"
                        aria-label="Mở hoặc đóng menu"
                    />

                    <Text
                        fw={700}
                        size="lg"
                        className={styles.logo}
                    >
                        HOME RENT
                    </Text>
                </Group>

                <Group gap="sm" wrap="nowrap">
                    <Group gap={6} visibleFrom="sm">
                        <Text size="xs" c="dimmed">
                            Chế độ:
                        </Text>
                    </Group>

                    <SegmentedControl
                        size="xs"
                        radius="sm"
                        value={role}
                        onChange={(value) =>
                            setRole(value as DemoRole)
                        }
                        data={[
                            {
                                label: 'Manager',
                                value: 'MANAGER',
                            },
                            {
                                label: 'Admin',
                                value: 'ADMIN',
                            },
                        ]}
                        aria-label="Chuyển đổi giao diện Manager và Admin"
                    />

                    <Text size="xs" c="dimmed" visibleFrom="md">
                        RR8
                    </Text>

                    <ActionIcon
                        variant="default"
                        size="lg"
                        radius="sm"
                        onClick={toggleColorScheme}
                        aria-label={
                            isDark
                                ? 'Chuyển sang giao diện sáng'
                                : 'Chuyển sang giao diện tối'
                        }
                        title={isDark ? 'Light Mode' : 'Dark Mode'}
                    >
                        {isDark ? (
                            <Sun size={18} />
                        ) : (
                            <Moon size={18} />
                        )}
                    </ActionIcon>
                </Group>
            </Group>
        </AppShell.Header>
    );
}
