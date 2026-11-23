
import {
    ActionIcon,
    AppShell,
    Burger,
    Group,
    Text,
    useMantineColorScheme,
} from '@mantine/core';

import { Moon, Sun } from 'lucide-react';

import styles from './Header.module.css';

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

    const isDark = colorScheme === 'dark';

    return (
        <AppShell.Header className={styles.header}>
            <Group h="100%" px="lg" justify="space-between">
                <Group gap="md">
                    <Burger
                        opened={opened}
                        onClick={onToggleMenu}
                        hiddenFrom="sm"
                        size="sm"
                        aria-label="Mở hoặc đóng menu"
                    />

                    <Text fw={700} size="lg" className={styles.logo}>
                        HOME RENT
                    </Text>
                </Group>

                <Group gap="md">
                    <Text size="sm" c="dimmed">
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
