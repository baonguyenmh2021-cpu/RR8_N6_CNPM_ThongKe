
import { useEffect, useState } from 'react';
import axios from 'axios';

import {
    Alert,
    Badge,
    Box,
    Button,
    Group,
    Loader,
    Paper,
    SimpleGrid,
    Stack,
    Table,
    Text,
    TextInput,
    Title,
} from '@mantine/core';

import DiscountNavigation from '../components/DiscountNavigation';

import {
    getDiscountEffectiveness,
} from '../api/discountEffectivenessApi';

import type {
    DiscountEffectivenessResponse,
} from '../types/DiscountEffectiveness';

import styles from './DiscountEffectivenessPage.module.css';

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
});

const numberFormatter = new Intl.NumberFormat('vi-VN');

function formatMoney(value: number | string): string {
    const amount = Number(value);

    return Number.isFinite(amount)
        ? moneyFormatter.format(amount)
        : 'Không hợp lệ';
}

function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        if (!error.response) {
            return 'Không kết nối được Spring Boot.';
        }

        if (error.response.status === 400) {
            return 'Khoảng thời gian thống kê không hợp lệ.';
        }

        if (
            error.response.status === 404 ||
            error.response.status === 405
        ) {
            return 'API US-06 chưa khả dụng. Kiểm tra profile local-demo.';
        }

        return `Backend trả về HTTP ${error.response.status}.`;
    }

    return error instanceof Error
        ? error.message
        : 'Không thể tải hiệu quả mã chiết khấu.';
}

function getStatusColor(status: string): string {
    switch (status.toLowerCase()) {
        case 'active':
            return 'green';
        case 'expired':
            return 'yellow';
        case 'disabled':
            return 'gray';
        default:
            return 'blue';
    }
}

function getStatusLabel(status: string): string {
    switch (status.toLowerCase()) {
        case 'active':
            return 'Còn hiệu lực';
        case 'expired':
            return 'Hết hạn';
        case 'disabled':
            return 'Đã vô hiệu hóa';
        default:
            return status;
    }
}

interface SearchPeriod {
    from: string;
    to: string;
    refresh: number;
}

export default function DiscountEffectivenessPage() {
    const [fromInput, setFromInput] = useState('');
    const [toInput, setToInput] = useState('');

    const [period, setPeriod] = useState<SearchPeriod>({
        from: '',
        to: '',
        refresh: 0,
    });

    const [data, setData] = useState<
        DiscountEffectivenessResponse[]
    >([]);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [filterError, setFilterError] =
        useState<string | null>(null);

    useEffect(() => {
        const controller = new AbortController();

        setLoading(true);
        setError(null);

        getDiscountEffectiveness(
            period.from || undefined,
            period.to || undefined,
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

    function handleSearch() {
        setFilterError(null);

        // Backend chỉ chấp nhận cả hai ngày hoặc không ngày nào.
        if ((fromInput === '') !== (toInput === '')) {
            setFilterError(
                'Bạn cần chọn cả ngày bắt đầu và ngày kết thúc.'
            );
            return;
        }

        // Khoảng [from, to), nên to phải lớn hơn from.
        if (fromInput && toInput && toInput <= fromInput) {
            setFilterError(
                'Ngày kết thúc phải sau ngày bắt đầu.'
            );
            return;
        }

        setPeriod((prev) => ({
            from: fromInput,
            to: toInput,
            refresh: prev.refresh + 1,
        }));
    }

    function handleShowAll() {
        setFromInput('');
        setToInput('');
        setFilterError(null);

        setPeriod((prev) => ({
            from: '',
            to: '',
            refresh: prev.refresh + 1,
        }));
    }

    // Các KPI đều được tính từ response backend.
    const totalUses = data.reduce(
        (sum, item) => sum + item.uses,
        0
    );

    const totalDiscount = data.reduce(
        (sum, item) => sum + Number(item.totalDiscount),
        0
    );

    const totalRevenue = data.reduce(
        (sum, item) => sum + Number(item.netRevenue),
        0
    );

    const maxRevenue = Math.max(
        0,
        ...data.map((item) => Number(item.netRevenue))
    );

    return (
        <Stack gap="lg">
            {/* Tiêu đề */}
            <Box>
                <Title order={2} className={styles.pageTitle}>
                    Hiệu quả mã chiết khấu
                </Title>

                <Text size="sm" c="dimmed" mt={4}>
                    Theo dõi lượt sử dụng, số tiền giảm và doanh thu
                    của từng mã chiết khấu.
                </Text>
            </Box>

            <DiscountNavigation />

            {/* Bộ lọc */}
            <Paper withBorder radius="sm" p="lg">
                <Stack gap="md">
                    <Text fw={600}>
                        Bộ lọc thời gian
                    </Text>

                    <Group align="end" gap="md" wrap="wrap">
                        <TextInput
                            type="date"
                            label="Từ ngày"
                            description="Tính từ ngày này"
                            value={fromInput}
                            onChange={(event) =>
                                setFromInput(event.currentTarget.value)
                            }
                            disabled={loading}
                            w={{ base: '100%', sm: 180 }}
                        />

                        <TextInput
                            type="date"
                            label="Đến trước ngày"
                            description="Không bao gồm ngày này"
                            value={toInput}
                            onChange={(event) =>
                                setToInput(event.currentTarget.value)
                            }
                            disabled={loading}
                            w={{ base: '100%', sm: 180 }}
                        />

                        <Button
                            onClick={handleSearch}
                            loading={loading}
                        >
                            Xem thống kê
                        </Button>

                        <Button
                            variant="default"
                            onClick={handleShowAll}
                            disabled={loading}
                        >
                            Toàn bộ
                        </Button>
                    </Group>

                    <Text size="xs" c="dimmed">
                        Khoảng ngày áp dụng theo thời điểm tạo booking,
                        không phải thời điểm thanh toán.
                    </Text>
                </Stack>
            </Paper>

            {filterError && (
                <Alert color="yellow" title="Bộ lọc chưa hợp lệ">
                    {filterError}
                </Alert>
            )}

            {loading && (
                <Paper withBorder radius="sm" p="xl">
                    <Group justify="center">
                        <Loader size="sm" />
                        <Text size="sm" c="dimmed">
                            Đang tải thống kê mã chiết khấu...
                        </Text>
                    </Group>
                </Paper>
            )}

            {error && !loading && (
                <Alert color="red" title="Không thể tải dữ liệu">
                    {error}
                </Alert>
            )}

            {!loading && !error && (
                <>
                    {/* KPI */}
                    <SimpleGrid
                        cols={{ base: 1, sm: 2, lg: 4 }}
                        spacing="md"
                    >
                        <Paper withBorder radius="sm" p="lg">
                            <Text size="xs" c="dimmed">
                                Số mã chiết khấu
                            </Text>
                            <Text className={styles.metricValue}>
                                {numberFormatter.format(data.length)}
                            </Text>
                        </Paper>

                        <Paper withBorder radius="sm" p="lg">
                            <Text size="xs" c="dimmed">
                                Tổng lượt sử dụng
                            </Text>
                            <Text className={styles.metricValue}>
                                {numberFormatter.format(totalUses)}
                            </Text>
                        </Paper>

                        <Paper withBorder radius="sm" p="lg">
                            <Text size="xs" c="dimmed">
                                Tổng tiền chiết khấu
                            </Text>
                            <Text className={styles.metricValue}>
                                {formatMoney(totalDiscount)}
                            </Text>
                        </Paper>

                        <Paper withBorder radius="sm" p="lg">
                            <Text size="xs" c="dimmed">
                                Doanh thu từ mã (Paid)
                            </Text>
                            <Text className={styles.metricValue}>
                                {formatMoney(totalRevenue)}
                            </Text>
                        </Paper>
                    </SimpleGrid>

                    {/* Biểu đồ */}
                    <Paper withBorder radius="sm" p="lg">
                        <Text fw={600} mb={4}>
                            Doanh thu theo từng mã
                        </Text>

                        <Text size="xs" c="dimmed" mb="lg">
                            So sánh doanh thu Paid của các mã chiết khấu.
                        </Text>

                        {data.length === 0 ? (
                            <Text size="sm" c="dimmed">
                                Chưa có mã chiết khấu trong hệ thống.
                            </Text>
                        ) : (
                            <Stack gap="lg" className={styles.chartList}>
                                {data.map((item) => {
                                    const revenue = Number(item.netRevenue);

                                    const width =
                                        maxRevenue > 0
                                            ? Math.max(
                                                0,
                                                Math.min(100, revenue / maxRevenue * 100)
                                            )
                                            : 0;

                                    return (
                                        <Box key={item.discountId}>
                                            <Group
                                                justify="space-between"
                                                gap="sm"
                                                mb={8}
                                            >
                                                <Text size="sm" fw={600}>
                                                    {item.code}
                                                </Text>

                                                <Text size="sm" fw={500}>
                                                    {formatMoney(item.netRevenue)}
                                                </Text>
                                            </Group>

                                            <div className={styles.barTrack}>
                                                <div
                                                    className={styles.barFill}
                                                    style={{ width: `${width}%` }}
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
                            Chi tiết hiệu quả mã chiết khấu
                        </Text>

                        <Table.ScrollContainer minWidth={820}>
                            <Table striped highlightOnHover>
                                <Table.Thead>
                                    <Table.Tr>
                                        <Table.Th>Mã</Table.Th>
                                        <Table.Th>Trạng thái</Table.Th>
                                        <Table.Th ta="right">Đã dùng</Table.Th>
                                        <Table.Th ta="right">Giới hạn</Table.Th>
                                        <Table.Th ta="right">Còn lại</Table.Th>
                                        <Table.Th ta="right">Tiền giảm</Table.Th>
                                        <Table.Th ta="right">Doanh thu</Table.Th>
                                    </Table.Tr>
                                </Table.Thead>

                                <Table.Tbody>
                                    {data.map((item) => (
                                        <Table.Tr key={item.discountId}>
                                            <Table.Td>
                                                <Text size="sm" fw={600}>
                                                    {item.code}
                                                </Text>
                                            </Table.Td>

                                            <Table.Td>
                                                <Badge
                                                    color={getStatusColor(item.effectiveStatus)}
                                                    variant="light"
                                                    size="sm"
                                                >
                                                    {getStatusLabel(item.effectiveStatus)}
                                                </Badge>
                                            </Table.Td>

                                            <Table.Td ta="right">
                                                {numberFormatter.format(item.uses)}
                                            </Table.Td>

                                            <Table.Td ta="right">
                                                {item.usageLimit === null
                                                    ? 'Không giới hạn'
                                                    : numberFormatter.format(item.usageLimit)}
                                            </Table.Td>

                                            <Table.Td ta="right">
                                                {item.remainingUses === null
                                                    ? '—'
                                                    : numberFormatter.format(item.remainingUses)}
                                            </Table.Td>

                                            <Table.Td ta="right">
                                                {formatMoney(item.totalDiscount)}
                                            </Table.Td>

                                            <Table.Td ta="right">
                                                {formatMoney(item.netRevenue)}
                                            </Table.Td>
                                        </Table.Tr>
                                    ))}
                                </Table.Tbody>
                            </Table>
                        </Table.ScrollContainer>

                        <Text size="xs" c="dimmed" mt="md">
                            Số lượt còn lại là giá trị do API trả về.
                            Khi lọc thời gian, backend hiện tính lượt
                            sử dụng trong khoảng đã chọn.
                        </Text>
                    </Paper>
                </>
            )}
        </Stack>
    );
}
