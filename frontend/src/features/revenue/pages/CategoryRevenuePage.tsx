
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
    Table,
    Text,
    Title,
} from '@mantine/core';

import RevenueNavigation from '../components/RevenueNavigation';
import { getCategoryRevenue } from '../api/categoryRevenueApi';

import type {
    CategoryRevenueResponse,
} from '../types/CategoryRevenue';

import type { ApiNumber } from '../types/MonthlyRevenue';

import styles from './CategoryRevenuePage.module.css';

// Danh sách tháng để lọc báo cáo
const months = Array.from({ length: 12 }, (_, index) => ({
    value: String(index + 1),
    label: `Tháng ${index + 1}`,
}));

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
});

const percentFormatter = new Intl.NumberFormat('vi-VN', {
    maximumFractionDigits: 2,
});

function formatMoney(value: ApiNumber): string {
    const number = Number(value);

    return Number.isFinite(number)
        ? moneyFormatter.format(number)
        : 'Không hợp lệ';
}

function formatPercent(value: number): string {
    return `${percentFormatter.format(value)}%`;
}

function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        if (!error.response) {
            return 'Không kết nối được Spring Boot. Hãy kiểm tra backend.';
        }

        if (error.response.status === 400) {
            return 'Tháng hoặc năm thống kê không hợp lệ.';
        }

        if (error.response.status === 404) {
            return 'Không tìm thấy API thống kê theo loại phòng.';
        }

        return `Backend trả về HTTP ${error.response.status}.`;
    }

    return error instanceof Error
        ? error.message
        : 'Không thể tải báo cáo doanh thu theo loại phòng.';
}

export default function CategoryRevenuePage() {
    const now = new Date();

    // Bộ lọc trên giao diện
    const [monthInput, setMonthInput] = useState(
        String(now.getMonth() + 1)
    );

    const [yearInput, setYearInput] = useState<string | number>(
        now.getFullYear()
    );

    // Kỳ thống kê được gửi tới Spring Boot
    const [period, setPeriod] = useState({
        year: now.getFullYear(),
        month: now.getMonth() + 1,
    });

    const [refreshKey, setRefreshKey] = useState(0);

    const [data, setData] =
        useState<CategoryRevenueResponse | null>(null);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const validYear =
        yearInput !== '' &&
        Number.isInteger(Number(yearInput)) &&
        Number(yearInput) >= 1 &&
        Number(yearInput) <= 9999;

    // Gọi API US-03
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError(null);
        setData(null);

        getCategoryRevenue(
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
    }, [period.year, period.month, refreshKey]);

    // Nhấn nút Xem báo cáo
    function handleSearch() {
        if (!validYear) return;

        setPeriod({
            year: Number(yearInput),
            month: Number(monthInput),
        });

        setRefreshKey((value) => value + 1);
    }

    const categories = data?.categories ?? [];

    const totalRevenue = data
        ? Number(data.totalRevenue)
        : 0;

    const totalBookings = categories.reduce(
        (sum, item) => sum + item.bookingCount,
        0
    );

    function getPercentage(revenue: ApiNumber): number {
        if (totalRevenue <= 0) return 0;

        return (Number(revenue) / totalRevenue) * 100;
    }

    return (
        <Stack gap="lg">
            {/* ===== TIÊU ĐỀ ===== */}
            <Box>
                <Title order={2} className={styles.pageTitle}>
                    Doanh thu theo loại phòng
                </Title>

                <Text c="dimmed" size="sm" mt={4}>
                    Phân tích doanh thu và số lượt đặt phòng
                    theo từng loại phòng trong Home Rent.
                </Text>
            </Box>

            {/* ===== ĐIỀU HƯỚNG US-01, US-02, US-03 ===== */}
            <RevenueNavigation />

            {/* ===== BỘ LỌC ===== */}
            <Paper withBorder radius="sm" p="md">
                <Group gap="md" align="end" wrap="wrap">
                    <Select
                        label="Tháng"
                        data={months}
                        value={monthInput}
                        onChange={(value) => {
                            if (value !== null) {
                                setMonthInput(value);
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
                        radius="sm"
                    >
                        Xem báo cáo
                    </Button>
                </Group>
            </Paper>

            {/* ===== TRẠNG THÁI ĐANG TẢI ===== */}
            {loading && (
                <Paper withBorder radius="sm" p="xl">
                    <Group justify="center" gap="sm">
                        <Loader size="sm" />
                        <Text size="sm" c="dimmed">
                            Đang tải thống kê loại phòng...
                        </Text>
                    </Group>
                </Paper>
            )}

            {/* ===== THÔNG BÁO LỖI ===== */}
            {error && !loading && (
                <Alert color="red" title="Không thể tải báo cáo">
                    {error}
                </Alert>
            )}

            {/* ===== DỮ LIỆU TỪ BACKEND ===== */}
            {data && !loading && !error && (
                <>
                    {/* Tổng quan */}
                    <SimpleGrid
                        cols={{ base: 1, sm: 3 }}
                        spacing="md"
                    >
                        <Paper withBorder radius="sm" p="lg">
                            <Text size="sm" c="dimmed">
                                Tổng doanh thu
                            </Text>

                            <Text className={styles.metricValue}>
                                {formatMoney(data.totalRevenue)}
                            </Text>

                            <Text size="xs" c="dimmed" mt={8}>
                                Tháng {data.month}/{data.year}
                            </Text>
                        </Paper>

                        <Paper withBorder radius="sm" p="lg">
                            <Text size="sm" c="dimmed">
                                Tổng booking
                            </Text>

                            <Text className={styles.metricValue}>
                                {totalBookings}
                            </Text>

                            <Text size="xs" c="dimmed" mt={8}>
                                Cộng từ các loại phòng
                            </Text>
                        </Paper>

                        <Paper withBorder radius="sm" p="lg">
                            <Text size="sm" c="dimmed">
                                Số loại phòng
                            </Text>

                            <Text className={styles.metricValue}>
                                {categories.length}
                            </Text>

                            <Text size="xs" c="dimmed" mt={8}>
                                Theo dữ liệu backend
                            </Text>
                        </Paper>
                    </SimpleGrid>

                    {/* Biểu đồ tỷ trọng */}
                    <Paper withBorder radius="sm" p="lg">
                        <Text fw={600} mb={4}>
                            Tỷ trọng doanh thu theo loại phòng
                        </Text>

                        <Text size="xs" c="dimmed" mb="lg">
                            Tỷ lệ được tính từ doanh thu từng loại
                            phòng so với tổng doanh thu tháng.
                        </Text>

                        {categories.length === 0 ? (
                            <Text size="sm" c="dimmed">
                                Chưa có loại phòng nào trong dữ liệu.
                            </Text>
                        ) : (
                            <Stack gap="lg">
                                {categories.map((item) => {
                                    const percentage = getPercentage(
                                        item.revenue
                                    );

                                    const barWidth = Math.max(
                                        0,
                                        Math.min(100, percentage)
                                    );

                                    return (
                                        <Box key={item.categoryId}>
                                            <Group
                                                justify="space-between"
                                                align="center"
                                                mb={8}
                                                wrap="wrap"
                                            >
                                                <Box>
                                                    <Text fw={600} size="sm">
                                                        {item.categoryName}
                                                    </Text>

                                                    <Text size="xs" c="dimmed">
                                                        {item.bookingCount} booking
                                                    </Text>
                                                </Box>

                                                <Box className={styles.rightText}>
                                                    <Text fw={600} size="sm">
                                                        {formatMoney(item.revenue)}
                                                    </Text>

                                                    <Text size="xs" c="dimmed">
                                                        {formatPercent(percentage)}
                                                    </Text>
                                                </Box>
                                            </Group>

                                            <div className={styles.barTrack}>
                                                <div
                                                    className={styles.barFill}
                                                    style={{
                                                        width: `${barWidth}%`,
                                                    }}
                                                />
                                            </div>
                                        </Box>
                                    );
                                })}
                            </Stack>
                        )}
                    </Paper>

                    {/* Bảng chi tiết */}
                    <Paper withBorder radius="sm" p="lg">
                        <Text fw={600} mb="md">
                            Chi tiết doanh thu từng loại phòng
                        </Text>

                        <Table.ScrollContainer minWidth={560}>
                            <Table striped highlightOnHover>
                                <Table.Thead>
                                    <Table.Tr>
                                        <Table.Th>Loại phòng</Table.Th>
                                        <Table.Th ta="right">
                                            Booking
                                        </Table.Th>
                                        <Table.Th ta="right">
                                            Doanh thu
                                        </Table.Th>
                                        <Table.Th ta="right">
                                            Tỷ trọng
                                        </Table.Th>
                                    </Table.Tr>
                                </Table.Thead>

                                <Table.Tbody>
                                    {categories.map((item) => {
                                        const percentage = getPercentage(
                                            item.revenue
                                        );

                                        return (
                                            <Table.Tr key={item.categoryId}>
                                                <Table.Td>
                                                    <Group gap="xs">
                                                        <Text size="sm">
                                                            {item.categoryName}
                                                        </Text>

                                                        {Number(item.revenue) === 0 && (
                                                            <Badge
                                                                size="xs"
                                                                color="gray"
                                                                variant="light"
                                                            >
                                                                0đ
                                                            </Badge>
                                                        )}
                                                    </Group>
                                                </Table.Td>

                                                <Table.Td ta="right">
                                                    {item.bookingCount}
                                                </Table.Td>

                                                <Table.Td ta="right">
                                                    {formatMoney(item.revenue)}
                                                </Table.Td>

                                                <Table.Td ta="right">
                                                    {formatPercent(percentage)}
                                                </Table.Td>
                                            </Table.Tr>
                                        );
                                    })}
                                </Table.Tbody>

                                <Table.Tfoot>
                                    <Table.Tr>
                                        <Table.Th>Tổng cộng</Table.Th>
                                        <Table.Th ta="right">
                                            {totalBookings}
                                        </Table.Th>
                                        <Table.Th ta="right">
                                            {formatMoney(data.totalRevenue)}
                                        </Table.Th>
                                        <Table.Th ta="right">
                                            {totalRevenue > 0 ? '100%' : '0%'}
                                        </Table.Th>
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
