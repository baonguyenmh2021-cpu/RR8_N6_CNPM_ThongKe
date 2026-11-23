
import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';

import {
    Alert,
    Badge,
    Box,
    Button,
    Divider,
    Group,
    Loader,
    Paper,
    Progress,
    SimpleGrid,
    Stack,
    Table,
    Text,
    TextInput,
    Title,
} from '@mantine/core';

import { getOccupancyCategories } from '../api/occupancyApi';

import type {
    OccupancyCategoryResponse,
} from '../types/Occupancy';

import styles from './OccupancyPage.module.css';

// Định dạng ngày YYYY-MM-DD theo giờ địa phương
function formatDateInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

// Mặc định thống kê tháng hiện tại
const today = new Date();

const initialFrom = formatDateInput(
    new Date(today.getFullYear(), today.getMonth(), 1)
);

const initialTo = formatDateInput(
    new Date(today.getFullYear(), today.getMonth() + 1, 1)
);

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
});

const numberFormatter = new Intl.NumberFormat('vi-VN');

function formatMoney(
    value: number | string | null
): string {
    if (value === null) return '—';

    const number = Number(value);

    return Number.isFinite(number)
        ? moneyFormatter.format(number)
        : 'Không hợp lệ';
}

function formatNumber(value: number): string {
    return numberFormatter.format(value);
}

function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        if (!error.response) {
            return 'Không kết nối được Spring Boot. Hãy kiểm tra backend và cấu hình proxy.';
        }

        const responseData = error.response.data as
            { message?: string } | undefined;

        return responseData?.message
            ?? `Backend trả về HTTP ${error.response.status}.`;
    }

    if (error instanceof Error) {
        return error.message;
    }

    return 'Không thể tải dữ liệu thống kê.';
}

export default function OccupancyPage() {
    const [from, setFrom] = useState(initialFrom);
    const [to, setTo] = useState(initialTo);

    const [period, setPeriod] = useState({
        from: initialFrom,
        to: initialTo,
    });

    const [data, setData] =
        useState<OccupancyCategoryResponse[]>([]);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [validationError, setValidationError] =
        useState<string | null>(null);

    // Gọi API thống kê
    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError(null);

        getOccupancyCategories(
            period.from,
            period.to,
            controller.signal
        )
            .then((result) => {
                if (!controller.signal.aborted) {
                    setData(result);
                }
            })
            .catch((err: unknown) => {
                if (!controller.signal.aborted) {
                    setData([]);
                    setError(getErrorMessage(err));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            });

        return () => controller.abort();
    }, [period]);

    // Tổng hợp các chỉ số từ dữ liệu Backend
    const summary = useMemo(() => {
        const availableRooms = data.reduce(
            (sum, item) => sum + item.availableRooms,
            0
        );

        const bookedNights = data.reduce(
            (sum, item) => sum + item.bookedNights,
            0
        );

        const totalCapacity = data.reduce(
            (sum, item) =>
                sum + item.availableRooms * item.periodNights,
            0
        );

        const paidRevenue = data.reduce(
            (sum, item) => sum + Number(item.paidRevenue || 0),
            0
        );

        const occupancyRate =
            totalCapacity > 0
                ? (bookedNights / totalCapacity) * 100
                : null;

        return {
            availableRooms,
            bookedNights,
            paidRevenue,
            occupancyRate,
        };
    }, [data]);

    // Kiểm tra bộ lọc
    function handleSearch() {
        setValidationError(null);

        if (!from || !to) {
            setValidationError(
                'Vui lòng nhập đầy đủ ngày bắt đầu và kết thúc.'
            );
            return;
        }

        if (to <= from) {
            setValidationError(
                'Ngày kết thúc phải sau ngày bắt đầu.'
            );
            return;
        }

        setPeriod({ from, to });
    }

    return (
        <Stack gap="lg" className={styles.page}>

            {/* Tiêu đề */}
            <Box>
                <Title order={2} className={styles.pageTitle}>
                    Tỷ lệ lấp đầy phòng
                </Title>

                <Text size="sm" c="dimmed" mt={5}>
                    Theo dõi tình trạng sử dụng phòng và doanh thu
                    theo từng loại phòng.
                </Text>
            </Box>

            {/* Bộ lọc */}
            <Paper withBorder radius="sm" p="md">
                <Stack gap="md">
                    <Text fw={600} size="sm">
                        Khoảng thời gian thống kê
                    </Text>

                    <Group align="end" gap="md" wrap="wrap">
                        <TextInput
                            label="Từ ngày"
                            type="date"
                            value={from}
                            onChange={(event) =>
                                setFrom(event.currentTarget.value)
                            }
                            w={190}
                        />

                        <TextInput
                            label="Đến ngày (không bao gồm)"
                            type="date"
                            value={to}
                            onChange={(event) =>
                                setTo(event.currentTarget.value)
                            }
                            w={230}
                        />

                        <Button
                            onClick={handleSearch}
                            loading={loading}
                        >
                            Xem thống kê
                        </Button>
                    </Group>

                    <Text size="xs" c="dimmed">
                        Ngày kết thúc không được tính vào kỳ thống kê.
                    </Text>

                    {validationError && (
                        <Alert color="red" title="Ngày không hợp lệ">
                            {validationError}
                        </Alert>
                    )}
                </Stack>
            </Paper>

            {/* Loading */}
            {loading && (
                <Paper withBorder radius="sm" p="xl">
                    <Group justify="center">
                        <Loader size="sm" />
                        <Text size="sm" c="dimmed">
                            Đang tải dữ liệu Occupancy...
                        </Text>
                    </Group>
                </Paper>
            )}

            {/* Error */}
            {error && !loading && (
                <Alert color="red" title="Không thể tải thống kê">
                    {error}
                </Alert>
            )}

            {/* Kết quả */}
            {!loading && !error && (
                <Stack gap="lg">

                    {/* Tổng quan */}
                    <Box>
                        <Group justify="space-between" mb="md">
                            <Title order={4}>
                                Tổng quan
                            </Title>

                            <Badge variant="light" color="blue">
                                {data.length} loại phòng
                            </Badge>
                        </Group>

                        <SimpleGrid
                            cols={{
                                base: 1,
                                xs: 2,
                                lg: 4,
                            }}
                            spacing="md"
                        >
                            <Paper
                                withBorder
                                radius="sm"
                                p="md"
                                className={styles.metricCard}
                            >
                                <Text size="xs" c="dimmed">
                                    Tỷ lệ lấp đầy chung
                                </Text>

                                <Text className={styles.metricValue}>
                                    {summary.occupancyRate === null
                                        ? 'N/A'
                                        : `${summary.occupancyRate.toFixed(2)}%`}
                                </Text>
                            </Paper>

                            <Paper
                                withBorder
                                radius="sm"
                                p="md"
                                className={styles.metricCard}
                            >
                                <Text size="xs" c="dimmed">
                                    Phòng khả dụng
                                </Text>

                                <Text className={styles.metricValue}>
                                    {formatNumber(summary.availableRooms)}
                                </Text>
                            </Paper>

                            <Paper
                                withBorder
                                radius="sm"
                                p="md"
                                className={styles.metricCard}
                            >
                                <Text size="xs" c="dimmed">
                                    Tổng đêm đã đặt
                                </Text>

                                <Text className={styles.metricValue}>
                                    {formatNumber(summary.bookedNights)}
                                </Text>
                            </Paper>

                            <Paper
                                withBorder
                                radius="sm"
                                p="md"
                                className={styles.metricCard}
                            >
                                <Text size="xs" c="dimmed">
                                    Doanh thu đã thanh toán
                                </Text>

                                <Text className={styles.revenueValue}>
                                    {formatMoney(summary.paidRevenue)}
                                </Text>
                            </Paper>
                        </SimpleGrid>
                    </Box>

                    <Divider />

                    {/* Biểu đồ tỷ lệ lấp đầy */}
                    <Box>
                        <Title order={4} mb="md">
                            Tỷ lệ lấp đầy theo loại phòng
                        </Title>

                        {data.length === 0 ? (
                            <Paper
                                withBorder
                                radius="sm"
                                p="xl"
                            >
                                <Text ta="center" c="dimmed" size="sm">
                                    Không có dữ liệu thống kê trong kỳ này.
                                </Text>
                            </Paper>
                        ) : (
                            <Paper
                                withBorder
                                radius="sm"
                                p="lg"
                            >
                                <Stack gap="lg">
                                    {data.map((item) => {
                                        const rate =
                                            item.occupancyRatePercent === null
                                                ? null
                                                : Number(
                                                    item.occupancyRatePercent
                                                );

                                        const validRate =
                                            rate !== null &&
                                            Number.isFinite(rate);

                                        return (
                                            <Box key={item.categoryId}>
                                                <Group
                                                    justify="space-between"
                                                    mb={7}
                                                >
                                                    <Text
                                                        fw={600}
                                                        size="sm"
                                                    >
                                                        {item.categoryName}
                                                    </Text>

                                                    <Text
                                                        size="sm"
                                                        fw={600}
                                                        c={
                                                            validRate
                                                                ? 'blue'
                                                                : 'dimmed'
                                                        }
                                                    >
                                                        {validRate
                                                            ? `${rate!.toFixed(2)}%`
                                                            : 'Không có phòng khả dụng'}
                                                    </Text>
                                                </Group>

                                                <Progress
                                                    value={
                                                        validRate
                                                            ? Math.min(
                                                                100,
                                                                Math.max(0, rate!)
                                                            )
                                                            : 0
                                                    }
                                                    size="md"
                                                    radius="sm"
                                                    color="blue"
                                                />

                                                <Text
                                                    size="xs"
                                                    c="dimmed"
                                                    mt={5}
                                                >
                                                    {formatNumber(
                                                        item.bookedNights
                                                    )}{' '}
                                                    đêm đã đặt /{' '}
                                                    {formatNumber(
                                                        item.availableRooms *
                                                        item.periodNights
                                                    )}{' '}
                                                    đêm khả dụng
                                                </Text>
                                            </Box>
                                        );
                                    })}
                                </Stack>
                            </Paper>
                        )}
                    </Box>

                    <Divider />

                    {/* Bảng chi tiết */}
                    <Box>
                        <Group
                            justify="space-between"
                            align="center"
                            mb="md"
                        >
                            <Box>
                                <Title order={4}>
                                    Chi tiết theo loại phòng
                                </Title>

                                <Text size="xs" c="dimmed" mt={4}>
                                    Kỳ thống kê: {period.from} đến {period.to}
                                </Text>
                            </Box>
                        </Group>

                        {data.length === 0 ? (
                            <Paper
                                withBorder
                                radius="sm"
                                p="xl"
                            >
                                <Text ta="center" c="dimmed" size="sm">
                                    Không có dữ liệu để hiển thị.
                                </Text>
                            </Paper>
                        ) : (
                            <Paper
                                withBorder
                                radius="sm"
                                p="md"
                            >
                                <Table.ScrollContainer minWidth={1100}>
                                    <Table
                                        striped
                                        highlightOnHover
                                        verticalSpacing="md"
                                        horizontalSpacing="md"
                                    >
                                        <Table.Thead>
                                            <Table.Tr>
                                                <Table.Th>
                                                    Loại phòng
                                                </Table.Th>

                                                <Table.Th ta="right">
                                                    Phòng khả dụng
                                                </Table.Th>

                                                <Table.Th ta="right">
                                                    Số đêm kỳ
                                                </Table.Th>

                                                <Table.Th ta="right">
                                                    Đêm đã đặt
                                                </Table.Th>

                                                <Table.Th ta="right">
                                                    Tỷ lệ lấp đầy
                                                </Table.Th>

                                                <Table.Th ta="right">
                                                    Doanh thu Paid
                                                </Table.Th>

                                                <Table.Th ta="right">
                                                    Giá thấp nhất
                                                </Table.Th>

                                                <Table.Th ta="right">
                                                    Giá cao nhất
                                                </Table.Th>
                                            </Table.Tr>
                                        </Table.Thead>

                                        <Table.Tbody>
                                            {data.map((item) => (
                                                <Table.Tr
                                                    key={item.categoryId}
                                                >
                                                    <Table.Td>
                                                        <Text
                                                            fw={600}
                                                            size="sm"
                                                        >
                                                            {item.categoryName}
                                                        </Text>
                                                    </Table.Td>

                                                    <Table.Td ta="right">
                                                        {formatNumber(
                                                            item.availableRooms
                                                        )}
                                                    </Table.Td>

                                                    <Table.Td ta="right">
                                                        {formatNumber(
                                                            item.periodNights
                                                        )}
                                                    </Table.Td>

                                                    <Table.Td ta="right">
                                                        {formatNumber(
                                                            item.bookedNights
                                                        )}
                                                    </Table.Td>

                                                    <Table.Td ta="right">
                                                        {formatRateValue(
                                                            item.occupancyRatePercent
                                                        )}
                                                    </Table.Td>

                                                    <Table.Td ta="right">
                                                        {formatMoney(
                                                            item.paidRevenue
                                                        )}
                                                    </Table.Td>

                                                    <Table.Td ta="right">
                                                        {formatMoney(
                                                            item.minCurrentPrice
                                                        )}
                                                    </Table.Td>

                                                    <Table.Td ta="right">
                                                        {formatMoney(
                                                            item.maxCurrentPrice
                                                        )}
                                                    </Table.Td>
                                                </Table.Tr>
                                            ))}
                                        </Table.Tbody>
                                    </Table>
                                </Table.ScrollContainer>
                            </Paper>
                        )}
                    </Box>
                </Stack>
            )}
        </Stack>
    );
}

function formatRateValue(
    value: number | string | null
): string {
    if (value === null) return 'N/A';

    const number = Number(value);

    return Number.isFinite(number)
        ? `${number.toFixed(2)}%`
        : 'Không hợp lệ';
}
