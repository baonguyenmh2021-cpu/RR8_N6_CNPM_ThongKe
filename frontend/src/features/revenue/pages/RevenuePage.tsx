
import { useEffect, useState } from 'react';
import axios from 'axios';

import {
    Alert,
    Box,
    Button,
    Divider,
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
import { getMonthlyRevenue } from '../api/revenueApi';

import type {
    ApiNumber,
    MonthlyRevenueResponse,
} from '../types/MonthlyRevenue';

import styles from './RevenuePage.module.css';

// Danh sách 12 tháng
const months = Array.from({ length: 12 }, (_, index) => ({
    value: String(index + 1),
    label: `Tháng ${index + 1}`,
}));

// Định dạng tiền Việt Nam
const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
});

const percentFormatter = new Intl.NumberFormat('vi-VN', {
    maximumFractionDigits: 1,
});

function formatMoney(value: ApiNumber): string {
    const number = Number(value);

    if (!Number.isFinite(number)) {
        return 'Không hợp lệ';
    }

    return moneyFormatter.format(number);
}

function formatPercent(value: ApiNumber | null): string {
    if (value === null) {
        return 'N/A';
    }

    const number = Number(value);

    if (!Number.isFinite(number)) {
        return 'Không hợp lệ';
    }

    const formatted = percentFormatter.format(number);

    return `${number > 0 ? '+' : ''}${formatted}%`;
}

// Xử lý thông báo lỗi API
function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        if (!error.response) {
            return 'Không kết nối được Spring Boot. Vui lòng kiểm tra backend.';
        }

        switch (error.response.status) {
            case 400:
                return 'Tháng hoặc năm thống kê không hợp lệ.';

            case 404:
                return 'Không tìm thấy API doanh thu.';

            case 500:
                return 'Máy chủ gặp lỗi khi xử lý dữ liệu doanh thu.';

            default:
                return `Backend trả về HTTP ${error.response.status}.`;
        }
    }

    if (error instanceof Error) {
        return error.message;
    }

    return 'Không thể tải dữ liệu doanh thu.';
}

export default function RevenuePage() {
    const now = new Date();

    // Giá trị đang chọn trên bộ lọc
    const [month, setMonth] = useState(
        String(now.getMonth() + 1)
    );

    const [yearInput, setYearInput] = useState<string | number>(
        now.getFullYear()
    );

    // Kỳ thống kê thực sự gửi đến backend
    const [period, setPeriod] = useState({
        month: now.getMonth() + 1,
        year: now.getFullYear(),
    });

    // Dữ liệu từ API Spring Boot
    const [data, setData] = useState<
        MonthlyRevenueResponse | null
    >(null);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Kiểm tra năm hợp lệ
    const validYear =
        yearInput !== '' &&
        Number.isInteger(Number(yearInput)) &&
        Number(yearInput) >= 1 &&
        Number(yearInput) <= 9999;

    // Gọi API khi kỳ thống kê thay đổi
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError(null);
        setData(null);

        getMonthlyRevenue(
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

        return () => {
            controller.abort();
        };
    }, [period.year, period.month]);

    // Nút Xem báo cáo
    function handleSearch() {
        if (!validYear) {
            return;
        }

        setPeriod({
            month: Number(month),
            year: Number(yearInput),
        });
    }

    return (
        <Stack gap="lg">
            {/* ===== TIÊU ĐỀ TRANG ===== */}
            <Box>
                <Title order={2} className={styles.pageTitle}>
                    Doanh thu theo tháng
                </Title>

                <Text c="dimmed" size="sm" mt={4}>
                    Thống kê và theo dõi doanh thu Home Rent
                    theo từng tháng.
                </Text>
            </Box>

            {/* ===== ĐIỀU HƯỚNG US-01 / US-02 ===== */}
            <RevenueNavigation />

            {/* ===== BỘ LỌC THÁNG / NĂM ===== */}
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
                        radius="sm"
                    >
                        Xem báo cáo
                    </Button>
                </Group>
            </Paper>

            {/* ===== ĐANG TẢI DỮ LIỆU ===== */}
            {loading && (
                <Paper withBorder radius="sm" p="xl">
                    <Group justify="center" gap="sm">
                        <Loader size="sm" />

                        <Text size="sm" c="dimmed">
                            Đang tải dữ liệu doanh thu...
                        </Text>
                    </Group>
                </Paper>
            )}

            {/* ===== THÔNG BÁO LỖI ===== */}
            {error && !loading && (
                <Alert
                    color="red"
                    title="Không thể tải báo cáo"
                    radius="sm"
                >
                    {error}
                </Alert>
            )}

            {/* ===== KẾT QUẢ DOANH THU ===== */}
            {data && !loading && !error && (
                <Stack gap="md">
                    <Paper withBorder radius="sm" p="lg">
                        <Group justify="space-between" mb="lg">
                            <Box>
                                <Text fw={600}>
                                    Báo cáo tháng {period.month}/{period.year}
                                </Text>

                                <Text size="xs" c="dimmed" mt={4}>
                                    Dữ liệu lấy từ hệ thống Spring Boot
                                </Text>
                            </Box>
                        </Group>

                        {/* Các chỉ số thống kê */}
                        <SimpleGrid
                            cols={{ base: 1, sm: 2, lg: 4 }}
                            spacing="lg"
                        >
                            <Box>
                                <Text size="xs" c="dimmed" mb={6}>
                                    Tổng doanh thu
                                </Text>

                                <Text className={styles.metricValue}>
                                    {formatMoney(data.revenue)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed" mb={6}>
                                    Số booking
                                </Text>

                                <Text className={styles.metricValue}>
                                    {data.bookingCount}
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
                                    Booking tạo trong tháng hiện đã hủy
                                </Text>

                                <Text className={styles.metricValue}>
                                    {data.cancelledBookingsCreatedInMonth}
                                </Text>
                            </Box>
                        </SimpleGrid>

                        <Divider my="lg" />

                        {/* So sánh với tháng trước */}
                        <Text fw={600} size="sm" mb="md">
                            So sánh với tháng trước
                        </Text>

                        <SimpleGrid
                            cols={{ base: 1, sm: 2 }}
                            spacing="lg"
                        >
                            <Box>
                                <Text size="xs" c="dimmed" mb={6}>
                                    Doanh thu tháng trước
                                </Text>

                                <Text fw={600} size="lg">
                                    {formatMoney(data.previousMonthRevenue)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed" mb={6}>
                                    Thay đổi doanh thu
                                </Text>

                                <Text fw={600} size="lg">
                                    {formatPercent(data.revenueChangePercent)}
                                </Text>
                            </Box>
                        </SimpleGrid>

                        {data.revenueChangePercent === null && (
                            <Text size="xs" c="dimmed" mt="md">
                                Không thể tính tỷ lệ thay đổi khi doanh thu
                                tháng trước bằng 0.
                            </Text>
                        )}
                    </Paper>
                </Stack>
            )}
        </Stack>
    );
}
