
import { useState } from 'react';
import axios from 'axios';

import {
    Alert,
    Badge,
    Box,
    Button,
    Divider,
    Group,
    NumberInput,
    Paper,
    Progress,
    SimpleGrid,
    Stack,
    Text,
    Title,
} from '@mantine/core';

import { Link } from 'react-router-dom';

import { getMarketingEffectiveness } from '../api/marketingEffectivenessApi';

import type {
    MarketingEffectivenessResponse,
} from '../types/MarketingEffectiveness';

import styles from '../../revenue/pages/RevenuePage.module.css';

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

function formatPercent(
    value: number | string | null
): string {
    if (value === null) {
        return 'Chưa đủ dữ liệu để tính';
    }

    const number = Number(value);

    if (!Number.isFinite(number)) {
        return 'Không hợp lệ';
    }

    return `${number > 0 ? '+' : ''}${number.toFixed(2)}%`;
}

function formatDate(value: string): string {
    const date = new Date(`${value}T00:00:00`);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleDateString('vi-VN');
}

function getStatusColor(status: string): string {
    switch (status.toLowerCase()) {
        case 'planned':
            return 'blue';
        case 'active':
            return 'green';
        case 'completed':
            return 'gray';
        default:
            return 'gray';
    }
}

function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const data = error.response?.data as
            { message?: string } | undefined;

        if (data?.message) {
            return data.message;
        }

        if (!error.response) {
            return 'Không kết nối được Backend Spring Boot.';
        }

        switch (error.response.status) {
            case 400:
                return 'Strategy ID không hợp lệ.';
            case 404:
                return 'Không tìm thấy chiến lược Marketing.';
            case 500:
                return 'Dữ liệu thống kê không nhất quán. Vui lòng kiểm tra Database.';
            default:
                return `Backend trả về HTTP ${error.response.status}.`;
        }
    }

    return error instanceof Error
        ? error.message
        : 'Không thể tải dữ liệu thống kê.';
}

function MetricCard({
    label,
    value,
    description,
}: {
    label: string;
    value: string;
    description?: string;
}) {
    return (
        <Paper withBorder radius="sm" p="md">
            <Text size="xs" c="dimmed" mb={8}>
                {label}
            </Text>

            <Text
                fw={700}
                size="xl"
                style={{ overflowWrap: 'anywhere' }}
            >
                {value}
            </Text>

            {description && (
                <Text size="xs" c="dimmed" mt={6}>
                    {description}
                </Text>
            )}
        </Paper>
    );
}

export default function MarketingEffectivenessPage() {
    const [strategyId, setStrategyId] =
        useState<string | number>('');

    const [data, setData] =
        useState<MarketingEffectivenessResponse | null>(null);

    const [loading, setLoading] = useState(false);

    const [error, setError] =
        useState<string | null>(null);

    const validId =
        strategyId !== '' &&
        Number.isSafeInteger(Number(strategyId)) &&
        Number(strategyId) > 0;

    async function handleSearch() {
        if (!validId || loading) return;

        setLoading(true);
        setError(null);
        setData(null);

        try {
            const result =
                await getMarketingEffectiveness(
                    Number(strategyId)
                );

            setData(result);
        } catch (err: unknown) {
            setError(getErrorMessage(err));
        } finally {
            setLoading(false);
        }
    }

    const achievement =
        data?.bookingAchievementPercent === null ||
            data?.bookingAchievementPercent === undefined
            ? null
            : Number(data.bookingAchievementPercent);

    const achievementProgress =
        achievement === null ||
            !Number.isFinite(achievement)
            ? 0
            : Math.min(100, Math.max(0, achievement));

    const revenueChange =
        data?.revenueChangePercent === null ||
            data?.revenueChangePercent === undefined
            ? null
            : Number(data.revenueChangePercent);

    return (
        <Stack gap="lg">
            {/* Header */}
            <Group justify="space-between" align="start">
                <Box>
                    <Title order={2} className={styles.pageTitle}>
                        Hiệu quả chiến lược Marketing
                    </Title>

                </Box>

                <Badge color="blue" variant="light">
                    MANAGER
                </Badge>
            </Group>

            {/* Navigation */}

            {/* Navigation */}
            <Group gap="xs" wrap="wrap">
                <Button
                    component={Link}
                    to="/marketing"
                    variant="subtle"
                    size="sm"
                >
                    Tạo chiến lược
                </Button>

                <Button
                    component={Link}
                    to="/marketing/list"
                    variant="subtle"
                    size="sm"
                >
                    Danh sách chiến lược
                </Button>

                <Button
                    variant="light"
                    size="sm"
                >
                    Hiệu quả Marketing
                </Button>
            </Group>


            {/* Search */}
            <Paper withBorder radius="sm" p="lg">
                <Stack gap="md">
                    <Box>
                        <Title order={4}>
                            Tra cứu chiến lược
                        </Title>

                        <Text size="xs" c="dimmed" mt={5}>
                            Nhập mã Strategy ID để xem kết quả
                            thống kê từ Database.
                        </Text>
                    </Box>

                    <Group align="end">
                        <NumberInput
                            label="Strategy ID"
                            placeholder="Ví dụ: 1"
                            value={strategyId}
                            onChange={(value) => {
                                setStrategyId(value);
                                setData(null);
                                setError(null);
                            }}
                            min={1}
                            max={Number.MAX_SAFE_INTEGER}
                            allowNegative={false}
                            allowDecimal={false}
                            w={220}
                        />

                        <Button
                            onClick={handleSearch}
                            loading={loading}
                            disabled={!validId}
                        >
                            Xem hiệu quả
                        </Button>
                    </Group>
                </Stack>
            </Paper>

            {loading && (
                <Alert color="blue">
                    Đang tải dữ liệu thống kê từ Backend...
                </Alert>
            )}

            {error && !loading && (
                <Alert
                    color="red"
                    title="Không thể tải hiệu quả chiến lược"
                >
                    {error}
                </Alert>
            )}

            {data && !loading && (
                <Stack gap="lg">
                    {/* Strategy information */}
                    <Paper withBorder radius="sm" p="lg">
                        <Group
                            justify="space-between"
                            align="start"
                            mb="md"
                        >
                            <Box>
                                <Text size="xs" c="dimmed">
                                    Chiến lược #{data.strategyId}
                                </Text>

                                <Title order={3} mt={5}>
                                    {data.title}
                                </Title>
                            </Box>

                            <Badge
                                color={getStatusColor(data.status)}
                                variant="light"
                            >
                                {data.status}
                            </Badge>
                        </Group>

                        <Divider mb="md" />

                        <SimpleGrid cols={{ base: 1, sm: 2 }}>
                            <Box>
                                <Text size="xs" c="dimmed">
                                    Ngày bắt đầu
                                </Text>

                                <Text fw={500}>
                                    {formatDate(data.startDate)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Ngày kết thúc
                                </Text>

                                <Text fw={500}>
                                    {formatDate(data.endDate)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Ngân sách
                                </Text>

                                <Text fw={500}>
                                    {formatMoney(data.budget)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Chi phí thực tế
                                </Text>

                                <Text fw={500}>
                                    {formatMoney(data.actualSpend)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Discount ID
                                </Text>

                                <Text fw={500}>
                                    {data.discountId ?? 'Không liên kết'}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Booking kỳ vọng
                                </Text>

                                <Text fw={500}>
                                    {data.expectedBookings ?? 'Chưa thiết lập'}
                                </Text>
                            </Box>
                        </SimpleGrid>
                    </Paper>

                    {/* Revenue */}
                    <Box>
                        <Title order={4} mb="md">
                            1. So sánh doanh thu
                        </Title>

                        <SimpleGrid cols={{ base: 1, sm: 2 }}>
                            <MetricCard
                                label="Doanh thu trước chiến dịch"
                                value={formatMoney(data.revenueBefore)}
                                description="Giai đoạn liền trước, cùng độ dài"
                            />

                            <MetricCard
                                label="Doanh thu trong chiến dịch"
                                value={formatMoney(data.revenueDuring)}
                                description="Tổng thanh toán Paid trong thời gian chiến dịch"
                            />
                        </SimpleGrid>

                        <Paper withBorder radius="sm" p="md" mt="md">
                            <Text size="sm" c="dimmed">
                                Tỷ lệ thay đổi doanh thu
                            </Text>

                            <Group mt={8} align="center">
                                <Title
                                    order={2}
                                    c={
                                        revenueChange === null
                                            ? 'dimmed'
                                            : revenueChange > 0
                                                ? 'green'
                                                : revenueChange < 0
                                                    ? 'red'
                                                    : 'dimmed'
                                    }
                                >
                                    {formatPercent(
                                        data.revenueChangePercent
                                    )}
                                </Title>

                                {revenueChange !== null && (
                                    <Badge
                                        color={
                                            revenueChange > 0
                                                ? 'green'
                                                : revenueChange < 0
                                                    ? 'red'
                                                    : 'gray'
                                        }
                                    >
                                        {revenueChange > 0
                                            ? 'Tăng'
                                            : revenueChange < 0
                                                ? 'Giảm'
                                                : 'Không đổi'}
                                    </Badge>
                                )}
                            </Group>
                        </Paper>
                    </Box>

                    {/* Bookings */}
                    <Box>
                        <Title order={4} mb="md">
                            2. So sánh Booking
                        </Title>

                        <SimpleGrid cols={{ base: 1, sm: 2 }}>
                            <MetricCard
                                label="Booking trước chiến dịch"
                                value={data.bookingsBefore.toLocaleString('vi-VN')}
                            />

                            <MetricCard
                                label="Booking trong chiến dịch"
                                value={data.bookingsDuring.toLocaleString('vi-VN')}
                            />
                        </SimpleGrid>
                    </Box>

                    {/* Achievement */}
                    <Box>
                        <Title order={4} mb="md">
                            3. Mức độ hoàn thành mục tiêu
                        </Title>

                        <Paper withBorder radius="sm" p="lg">
                            {achievement === null ? (
                                <Alert color="yellow">
                                    Chưa thể tính tỷ lệ hoàn thành.
                                    Chiến lược chưa thiết lập Booking
                                    kỳ vọng hoặc mục tiêu bằng 0.
                                </Alert>
                            ) : (
                                <Stack gap="md">
                                    <Group justify="space-between">
                                        <Box>
                                            <Text size="xs" c="dimmed">
                                                Booking thực tế / kỳ vọng
                                            </Text>

                                            <Title order={2}>
                                                {data.bookingsDuring.toLocaleString('vi-VN')}
                                                {' / '}
                                                {data.expectedBookings?.toLocaleString('vi-VN')}
                                            </Title>
                                        </Box>

                                        <Title
                                            order={2}
                                            c={achievement >= 100
                                                ? 'green'
                                                : 'blue'}
                                        >
                                            {achievement.toFixed(2)}%
                                        </Title>
                                    </Group>

                                    <Progress
                                        value={achievementProgress}
                                        color={
                                            achievement >= 100
                                                ? 'green'
                                                : 'blue'
                                        }
                                        size="lg"
                                        radius="xl"
                                    />

                                    <Text size="xs" c="dimmed">
                                        {achievement >= 100
                                            ? 'Đã hoàn thành hoặc vượt mục tiêu.'
                                            : 'Chưa đạt mục tiêu Booking kỳ vọng.'}
                                    </Text>
                                </Stack>
                            )}
                        </Paper>
                    </Box>

                    {/* Discount */}
                    <Box>
                        <Title order={4} mb="md">
                            4. Hiệu quả mã Discount
                        </Title>

                        <MetricCard
                            label="Booking sử dụng Discount liên kết"
                            value={data.bookingsWithCode.toLocaleString('vi-VN')}
                            description={
                                data.discountId === null
                                    ? 'Chiến lược không liên kết mã Discount.'
                                    : `Discount ID: ${data.discountId}`
                            }
                        />
                    </Box>

                    <Alert color="blue" title="Lưu ý khi đánh giá">
                        Doanh thu được tính từ các payment có
                        trạng thái Paid. Booking được đếm theo
                        lần thanh toán Paid đầu tiên để tránh
                        đếm trùng. Đây là số liệu thống kê
                        tương quan trước và trong chiến dịch,
                        không tự khẳng định toàn bộ thay đổi
                        doanh thu là do Marketing tạo ra.
                    </Alert>
                </Stack>
            )}
        </Stack>
    );
}
