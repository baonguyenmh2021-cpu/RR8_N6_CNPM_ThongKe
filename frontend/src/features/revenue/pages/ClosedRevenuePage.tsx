
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
    Select,
    SimpleGrid,
    Stack,
    Text,
    Title,
} from '@mantine/core';

import RevenueNavigation from '../components/RevenueNavigation';
import { getClosedMonthlyReport } from '../api/closedRevenueApi';

import type {
    ClosedMonthlyReportResponse,
} from '../types/ClosedMonthlyReport';

import styles from './RevenuePage.module.css';

const months = Array.from({ length: 12 }, (_, index) => ({
    value: String(index + 1),
    label: `Tháng ${index + 1}`,
}));

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
});

function formatMoney(value: number | string): string {
    const number = Number(value);

    return Number.isFinite(number)
        ? moneyFormatter.format(number)
        : 'Không hợp lệ';
}

function formatDate(value: string): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value || 'Chưa có thông tin';
    }

    return date.toLocaleString('vi-VN');
}

function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        if (!error.response) {
            return 'Không kết nối được Spring Boot.';
        }

        if (error.response.status === 400) {
            return 'Tháng hoặc năm không hợp lệ.';
        }

        return `Backend trả về HTTP ${error.response.status}.`;
    }

    return error instanceof Error
        ? error.message
        : 'Không thể đọc báo cáo đã chốt.';
}

export default function ClosedRevenuePage() {
    const now = new Date();

    const [month, setMonth] = useState(
        String(now.getMonth() + 1)
    );

    const [yearInput, setYearInput] = useState<string | number>(
        now.getFullYear()
    );

    const [period, setPeriod] = useState({
        year: now.getFullYear(),
        month: now.getMonth() + 1,
    });

    const [refreshKey, setRefreshKey] = useState(0);

    const [data, setData] =
        useState<ClosedMonthlyReportResponse | null>(null);

    const [loading, setLoading] = useState(false);
    const [notFound, setNotFound] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const validYear =
        yearInput !== '' &&
        Number.isInteger(Number(yearInput)) &&
        Number(yearInput) >= 1 &&
        Number(yearInput) <= 9999;

    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setData(null);
        setError(null);
        setNotFound(false);

        getClosedMonthlyReport(
            period.year,
            period.month,
            controller.signal
        )
            .then((result) => {
                if (!controller.signal.aborted) {
                    setData(result);
                }
            })
            .catch((err: unknown) => {
                if (controller.signal.aborted) return;

                if (
                    axios.isAxiosError(err) &&
                    err.response?.status === 404
                ) {
                    setNotFound(true);
                } else {
                    setError(getErrorMessage(err));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [period.year, period.month, refreshKey]);

    function handleSearch() {
        if (!validYear) return;

        setPeriod({
            year: Number(yearInput),
            month: Number(month),
        });

        setRefreshKey((value) => value + 1);
    }

    return (
        <Stack gap="lg">
            <Box>
                <Title order={2} className={styles.pageTitle}>
                    Báo cáo doanh thu đã chốt
                </Title>

                <Text size="sm" c="dimmed" mt={4}>
                    Tra cứu dữ liệu snapshot được lưu trong
                    hệ thống theo từng tháng.
                </Text>
            </Box>

            <RevenueNavigation />

            {/* Bộ lọc */}
            <Paper withBorder radius="sm" p="md">
                <Group gap="md" align="end" wrap="wrap">
                    <Select
                        label="Tháng"
                        data={months}
                        value={month}
                        onChange={(value) => {
                            if (value !== null) {
                                setMonth(value);
                            }
                        }}
                        allowDeselect={false}
                        w={150}
                    />

                    <NumberInput
                        label="Năm"
                        value={yearInput}
                        onChange={setYearInput}
                        allowDecimal={false}
                        allowNegative={false}
                        min={1}
                        max={9999}
                        w={140}
                    />

                    <Button
                        onClick={handleSearch}
                        disabled={!validYear}
                        loading={loading}
                    >
                        Xem báo cáo
                    </Button>
                </Group>
            </Paper>

            {/* Loading */}
            {loading && (
                <Paper withBorder radius="sm" p="xl">
                    <Group justify="center">
                        <Loader size="sm" />
                        <Text size="sm" c="dimmed">
                            Đang tìm báo cáo đã chốt...
                        </Text>
                    </Group>
                </Paper>
            )}

            {/* Kỳ chưa có bản chốt */}
            {notFound && !loading && (
                <Alert color="yellow" title="Chưa có báo cáo đã chốt">
                    Chưa tìm thấy snapshot cho tháng {period.month}/
                    {period.year}. Đây là trạng thái tra cứu, không có
                    thao tác ghi dữ liệu nào được thực hiện.
                </Alert>
            )}

            {/* Lỗi API */}
            {error && !loading && (
                <Alert color="red" title="Không thể tải báo cáo">
                    {error}
                </Alert>
            )}

            {/* Snapshot có trong database */}
            {data && !loading && !error && (
                <Stack gap="md">
                    <Paper withBorder radius="sm" p="lg">
                        <Group justify="space-between" mb="lg">
                            <Box>
                                <Text fw={600}>
                                    Báo cáo tháng {data.month}/{data.year}
                                </Text>

                                <Text size="xs" c="dimmed" mt={4}>
                                    Mã báo cáo: #{data.reportId}
                                </Text>
                            </Box>

                            <Badge color="green" variant="light">
                                Đã chốt
                            </Badge>
                        </Group>

                        <SimpleGrid
                            cols={{ base: 1, sm: 2, lg: 4 }}
                            spacing="lg"
                        >
                            <Box>
                                <Text size="xs" c="dimmed" mb={6}>
                                    Doanh thu đã chốt
                                </Text>
                                <Text className={styles.metricValue}>
                                    {formatMoney(data.totalRevenue)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed" mb={6}>
                                    Tổng chiết khấu
                                </Text>
                                <Text className={styles.metricValue}>
                                    {formatMoney(data.totalDiscount)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed" mb={6}>
                                    Tổng booking
                                </Text>
                                <Text className={styles.metricValue}>
                                    {data.bookingCount}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed" mb={6}>
                                    Booking đã hủy
                                </Text>
                                <Text className={styles.metricValue}>
                                    {data.cancelledBookings}
                                </Text>
                            </Box>
                        </SimpleGrid>
                    </Paper>

                    <Paper withBorder radius="sm" p="lg">
                        <Text fw={600} mb="md">
                            Thông tin chốt báo cáo
                        </Text>

                        <Stack gap="sm">
                            <Group justify="space-between">
                                <Text size="sm" c="dimmed">
                                    Người thực hiện
                                </Text>
                                <Text size="sm" fw={500}>
                                    User ID: {data.generatedBy}
                                </Text>
                            </Group>

                            <Group justify="space-between">
                                <Text size="sm" c="dimmed">
                                    Thời điểm chốt
                                </Text>
                                <Text size="sm" fw={500}>
                                    {formatDate(data.generatedAt)}
                                </Text>
                            </Group>
                        </Stack>
                    </Paper>

                    <Alert color="blue" title="Dữ liệu snapshot">
                        Các giá trị ở đây là báo cáo đã lưu tại thời điểm
                        chốt, không phải doanh thu được tính lại theo
                        thời gian thực.
                    </Alert>
                </Stack>
            )}
        </Stack>
    );
}