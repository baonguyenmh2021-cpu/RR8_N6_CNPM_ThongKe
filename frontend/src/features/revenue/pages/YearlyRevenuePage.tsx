
import { useEffect, useState } from 'react';
import axios from 'axios';

import {
    Alert,
    Badge,
    Box,
    Button,
    Group,
    Loader,
    NumberInput,
    Paper,
    Stack,
    Table,
    Text,
    Title,
} from '@mantine/core';



import { getYearlyRevenue } from '../api/yearlyRevenueApi';
import RevenueNavigation from '../components/RevenueNavigation';
import type { YearlyRevenueResponse } from '../types/YearlyRevenue';

import styles from './YearlyRevenuePage.module.css';

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
});

function formatMoney(value: number | string) {
    return moneyFormatter.format(Number(value));
}

function getErrorMessage(error: unknown) {
    if (axios.isAxiosError(error)) {
        if (!error.response) {
            return 'Không kết nối được Spring Boot.';
        }

        if (error.response.status === 400) {
            return 'Năm thống kê không hợp lệ.';
        }

        return `Backend trả về HTTP ${error.response.status}.`;
    }

    return error instanceof Error
        ? error.message
        : 'Không thể tải dữ liệu doanh thu.';
}

export default function YearlyRevenuePage() {
    const currentYear = new Date().getFullYear();

    const [yearInput, setYearInput] = useState<string | number>(
        currentYear
    );
    const [year, setYear] = useState(currentYear);
    const [refreshKey, setRefreshKey] = useState(0);

    const [data, setData] =
        useState<YearlyRevenueResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const validYear =
        Number.isInteger(Number(yearInput)) &&
        Number(yearInput) >= 1 &&
        Number(yearInput) <= 9999;

    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError(null);
        setData(null);

        getYearlyRevenue(year, controller.signal)
            .then((result) => {
                if (!controller.signal.aborted) {
                    setData(result);
                }
            })
            .catch((err: unknown) => {
                if (!controller.signal.aborted) {
                    setError(getErrorMessage(err));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [year, refreshKey]);

    function handleSearch() {
        if (!validYear) return;

        setYear(Number(yearInput));
        setRefreshKey((value) => value + 1);
    }

    const months = data
        ? [...data.months].sort((a, b) => a.month - b.month)
        : [];

    const maxRevenue = Math.max(
        0,
        ...months.map((item) => Number(item.revenue))
    );

    const highestMonths = data?.highestRevenueMonths ?? [];

    return (
        <Stack gap="lg">
            <Box>
                <Title order={2} className={styles.pageTitle}>
                    Doanh thu theo năm
                </Title>

                <Text size="sm" c="dimmed" mt={4}>
                    Theo dõi xu hướng doanh thu 12 tháng của Home Rent.
                </Text>
            </Box>

            <RevenueNavigation />

            <Paper withBorder radius="sm" p="md">
                <Group align="end" gap="md">
                    <NumberInput
                        label="Năm thống kê"
                        value={yearInput}
                        onChange={setYearInput}
                        min={1}
                        max={9999}
                        allowDecimal={false}
                        allowNegative={false}
                        w={160}
                    />

                    <Button
                        onClick={handleSearch}
                        loading={loading}
                        disabled={!validYear}
                    >
                        Xem báo cáo
                    </Button>
                </Group>
            </Paper>

            {loading && (
                <Paper withBorder radius="sm" p="xl">
                    <Group justify="center">
                        <Loader size="sm" />
                        <Text size="sm" c="dimmed">
                            Đang tải doanh thu năm...
                        </Text>
                    </Group>
                </Paper>
            )}

            {error && !loading && (
                <Alert color="red" title="Không thể tải báo cáo">
                    {error}
                </Alert>
            )}

            {data && !loading && !error && (
                <>
                    <Paper withBorder radius="sm" p="lg">
                        <Text size="sm" c="dimmed">
                            Tổng doanh thu năm {data.year}
                        </Text>

                        <Title order={2} mt={6}>
                            {formatMoney(data.totalRevenue)}
                        </Title>

                        <Text size="sm" c="dimmed" mt="sm">
                            {maxRevenue === 0
                                ? 'Không có doanh thu trong năm đã chọn.'
                                : `Tháng doanh thu cao nhất: ${highestMonths
                                    .map((month) => `tháng ${month}`)
                                    .join(', ')}.`}
                        </Text>
                    </Paper>

                    <Paper withBorder radius="sm" p="lg">
                        <Text fw={600} mb="md">
                            Biểu đồ doanh thu 12 tháng
                        </Text>

                        <div className={styles.chart}>
                            {months.map((item) => {
                                const revenue = Number(item.revenue);

                                const height =
                                    maxRevenue > 0
                                        ? (revenue / maxRevenue) * 100
                                        : 0;

                                const highest =
                                    highestMonths.includes(item.month) &&
                                    maxRevenue > 0;

                                return (
                                    <div
                                        className={styles.chartItem}
                                        key={item.month}
                                    >
                                        <div className={styles.chartTrack}>
                                            <div
                                                className={
                                                    highest
                                                        ? `${styles.bar} ${styles.highestBar}`
                                                        : styles.bar
                                                }
                                                style={{ height: `${height}%` }}
                                                title={`Tháng ${item.month}: ${formatMoney(revenue)}`}
                                            />
                                        </div>

                                        <Text size="xs" c="dimmed">
                                            T{item.month}
                                        </Text>
                                    </div>
                                );
                            })}
                        </div>

                        <Text size="xs" c="dimmed" mt="md">
                            Cột xanh đậm biểu thị tháng có doanh thu
                            cao nhất. Di chuột lên cột để xem giá trị.
                        </Text>
                    </Paper>

                    <Paper withBorder radius="sm" p="lg">
                        <Text fw={600} mb="md">
                            Chi tiết doanh thu từng tháng
                        </Text>

                        <Table.ScrollContainer minWidth={450}>
                            <Table striped highlightOnHover>
                                <Table.Thead>
                                    <Table.Tr>
                                        <Table.Th>Tháng</Table.Th>
                                        <Table.Th ta="right">
                                            Doanh thu
                                        </Table.Th>
                                        <Table.Th>Ghi chú</Table.Th>
                                    </Table.Tr>
                                </Table.Thead>

                                <Table.Tbody>
                                    {months.map((item) => {
                                        const highest =
                                            highestMonths.includes(item.month) &&
                                            maxRevenue > 0;

                                        return (
                                            <Table.Tr key={item.month}>
                                                <Table.Td>
                                                    Tháng {item.month}
                                                </Table.Td>

                                                <Table.Td ta="right">
                                                    {formatMoney(item.revenue)}
                                                </Table.Td>

                                                <Table.Td>
                                                    {highest && (
                                                        <Badge
                                                            variant="light"
                                                            color="blue"
                                                            size="sm"
                                                        >
                                                            Cao nhất
                                                        </Badge>
                                                    )}
                                                </Table.Td>
                                            </Table.Tr>
                                        );
                                    })}
                                </Table.Tbody>

                                <Table.Tfoot>
                                    <Table.Tr>
                                        <Table.Th>Tổng năm</Table.Th>
                                        <Table.Th ta="right">
                                            {formatMoney(data.totalRevenue)}
                                        </Table.Th>
                                        <Table.Th />
                                    </Table.Tr>
                                </Table.Tfoot>
                            </Table>
                        </Table.ScrollContainer>
                    </Paper>
                </>
            )}
        </Stack>
    );
}
