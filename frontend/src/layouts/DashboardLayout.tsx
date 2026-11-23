
import { AppShell, Box, Container } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { Outlet } from 'react-router-dom';

import Header from '../components/layout/Header';
import Sidebar from '../components/layout/Sidebar';
import AppBreadcrumbs from '../components/layout/AppBreadcrumbs';

import styles from './DashboardLayout.module.css';

export default function DashboardLayout() {
    const [opened, { toggle, close }] = useDisclosure(false);

    return (
        <AppShell
            header={{ height: 64 }}
            navbar={{
                width: 250,
                breakpoint: 'sm',
                collapsed: { mobile: !opened },
            }}
            padding="lg"
        >
            <Header
                opened={opened}
                onToggleMenu={toggle}
            />

            <Sidebar onNavigate={close} />

            <AppShell.Main className={styles.main}>
                <Container size="xl" px={0}>
                    <AppBreadcrumbs />

                    <Box mt="lg">
                        <Outlet />
                    </Box>
                </Container>
            </AppShell.Main>
        </AppShell>
    );
}
