import { useEffect, useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import MarketingStrategyActions from '../components/MarketingStrategyActions';

import {
    Alert,
    Badge,
    Box,
    Button,
    Group,
    Loader,
    Paper,
    Select,
    Stack,
    Table,
    Text,
    Title,
} from '@mantine/core';

import { getMarketingStrategies } from '../api/marketingStrategyApi';

import type {
    MarketingStrategySummaryResponse,
    MarketingStrategyStatus,
} from '../types/MarketingStrategy';

import styles from '../../revenue/pages/RevenuePage.module.css';

type FilterValue = 'ALL' | MarketingStrategyStatus;

const filterOptions = [
    { value: 'ALL', label: 'Tất cả chiến lược' },
    { value: 'Planned', label: 'Planned - Đã lên kế hoạch' },
    { value: 'Running', label: 'Running - Đang triển khai' },
    { value: 'Finished', label: 'Finished - Đã hoàn thành' },
    { value: 'Cancelled', label: 'Cancelled - Đã hủy' },
];

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
    const date = new Date(`${value}T00:00:00`);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleDateString('vi-VN');
}

function getStatusColor(status: string): string {
    switch (status) {
        case 'Planned':
            return 'blue';
        case 'Running':
            return 'green';
        case 'Finished':
            return 'gray';
        case 'Cancelled':
            return 'red';
        default:
            return 'dark';
    }
}

function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const responseData = error.response?.data as
            | { message?: string }
            | undefined;

        if (responseData?.message) {
            return responseData.message;
        }

        if (!error.response) {
            return 'Không kết nối được Backend Spring Boot.';
        }

        return `Backend trả về HTTP ${error.response.status}.`;
    }

    return error instanceof Error
        ? error.message
        : 'Không thể tải danh sách chiến lược.';
}

export default function MarketingStrategyListPage() {
    const [filter, setFilter] = useState<FilterValue>('ALL');

    const [strategies, setStrategies] = useState<
        MarketingStrategySummaryResponse[]
    >([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [refreshKey, setRefreshKey] = useState(0);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    function handleStrategyUpdated(message: string) {
        setSuccessMessage(message);
        setRefreshKey((previous) => previous + 1);
    }

    useEffect(() => {
        const controller = new AbortController();

        async function loadStrategies() {
            setLoading(true);
            setError(null);

            try {
                const result = await getMarketingStrategies(
                    filter === 'ALL' ? undefined : filter,
                    controller.signal
                );

                if (!controller.signal.aborted) {
                    setStrategies(result);
                }
            } catch (err: unknown) {
                if (controller.signal.aborted) return;

                setError(getErrorMessage(err));
            } finally {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            }
        }

        loadStrategies();

        return () => controller.abort();
    }, [filter, refreshKey]);

    return (
        <Stack gap="lg">
            {/* Header */}
            <Group justify="space-between" align="start">
                <Box>
                    <Title order={2} className={styles.pageTitle}>
                        Danh sách chiến lược Marketing
                    </Title>
                </Box>

                <Badge color="blue" variant="light">
                    MANAGER
                </Badge>
            </Group>

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

                <Button variant="light" size="sm">
                    Danh sách chiến lược
                </Button>

                <Button
                    component={Link}
                    to="/marketing/effectiveness"
                    variant="subtle"
                    size="sm"
                >
                    Hiệu quả Marketing
                </Button>
            </Group>

            {/* Filter */}
            <Paper withBorder radius="sm" p="lg">
                <Group justify="space-between" align="end">
                    <Box>
                        <Title order={4}>Bộ lọc chiến lược</Title>

                        <Text size="xs" c="dimmed" mt={5}>
                            Lọc danh sách theo trạng thái triển khai.
                        </Text>
                    </Box>

                    <Select
                        label="Trạng thái"
                        data={filterOptions}
                        value={filter}
                        onChange={(value) => {
                            setFilter((value as FilterValue | null) ?? 'ALL');
                        }}
                        allowDeselect={false}
                        w={280}
                    />
                </Group>
            </Paper>

            {successMessage && (
                <Alert color="green" title="Thao tác thành công">
                    {successMessage}
                </Alert>
            )}

            {/* Loading */}
            {loading && (
                <Paper withBorder radius="sm" p="xl">
                    <Group justify="center">
                        <Loader size="sm" />

                        <Text size="sm" c="dimmed">
                            Đang tải danh sách chiến lược...
                        </Text>
                    </Group>
                </Paper>
            )}

            {/* Error */}
            {error && !loading && (
                <Alert color="red" title="Không thể tải danh sách">
                    {error}
                </Alert>
            )}

            {/* Data */}
            {!loading && !error && (
                <Paper withBorder radius="sm" p="lg">
                    <Group justify="space-between" mb="md">
                        <Title order={4}>Kết quả chiến lược</Title>

                        <Badge color="blue" variant="light">
                            {strategies.length} chiến lược
                        </Badge>
                    </Group>

                    {strategies.length === 0 ? (
                        <Alert color="yellow" title="Không có dữ liệu">
                            Không tìm thấy chiến lược phù hợp với trạng thái
                            đang chọn.
                        </Alert>
                    ) : (
                        <Box style={{ overflowX: 'auto' }}>
                            <Table
                                striped
                                highlightOnHover
                                withTableBorder
                                miw={850}
                            >
                                <Table.Thead>
                                    <Table.Tr>
                                        <Table.Th>ID</Table.Th>
                                        <Table.Th>Tên chiến lược</Table.Th>
                                        <Table.Th>Đối tượng</Table.Th>
                                        <Table.Th>Thời gian</Table.Th>
                                        <Table.Th>Ngân sách</Table.Th>
                                        <Table.Th>Trạng thái</Table.Th>
                                        <Table.Th>Thao tác</Table.Th>
                                    </Table.Tr>
                                </Table.Thead>

                                <Table.Tbody>
                                    {strategies.map((item) => (
                                        <Table.Tr key={item.strategyId}>
                                            <Table.Td>
                                                #{item.strategyId}
                                            </Table.Td>

                                            <Table.Td>
                                                <Text fw={600} size="sm">
                                                    {item.title}
                                                </Text>
                                            </Table.Td>

                                            <Table.Td>
                                                {item.targetSegment ||
                                                    'Chưa xác định'}
                                            </Table.Td>

                                            <Table.Td>
                                                <Text size="xs">
                                                    {formatDate(item.startDate)}
                                                </Text>

                                                <Text size="xs" c="dimmed">
                                                    đến{' '}
                                                    {formatDate(item.endDate)}
                                                </Text>
                                            </Table.Td>

                                            <Table.Td>
                                                <Text size="sm" fw={500}>
                                                    {formatMoney(item.budget)}
                                                </Text>
                                            </Table.Td>

                                            <Table.Td>
                                                <Badge
                                                    color={getStatusColor(
                                                        item.status
                                                    )}
                                                    variant="light"
                                                >
                                                    {item.status}
                                                </Badge>
                                            </Table.Td>

                                            <Table.Td>
                                                <Group gap="xs" wrap="nowrap">
                                                    <Button
                                                        component={Link}
                                                        to="/marketing/effectiveness"
                                                        size="xs"
                                                        variant="light"
                                                    >
                                                        Đánh giá
                                                    </Button>

                                                    <MarketingStrategyActions
                                                        item={item}
                                                        onUpdated={
                                                            handleStrategyUpdated
                                                        }
                                                    />
                                                </Group>
                                            </Table.Td>
                                        </Table.Tr>
                                    ))}
                                </Table.Tbody>
                            </Table>
                        </Box>
                    )}
                </Paper>
            )}
        </Stack>
    );
}