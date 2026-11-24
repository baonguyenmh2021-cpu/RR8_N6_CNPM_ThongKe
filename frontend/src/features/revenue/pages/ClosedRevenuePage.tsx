
import { useEffect, useState } from 'react';
import axios from 'axios';

import {
    Alert,
    Badge,
    Box,
    Button,
    Group,
    Loader,
    Modal,
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
import { closeMonthlyReport } from '../api/closeMonthlyReportApi';

import { useDemoRole } from '../../auth/hooks/useDemoRole';

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
            return 'Không kết nối được Spring Boot. Hãy kiểm tra Backend.';
        }

        const status = error.response.status;

        if (status === 400) {
            return 'Tháng hoặc năm không hợp lệ.';
        }

        if (status === 403) {
            return 'Bạn không có quyền thực hiện thao tác này.';
        }

        if (status === 409) {
            return 'Kỳ báo cáo đã tồn tại hoặc đã được chốt trước đó.';
        }

        return `Backend trả về lỗi HTTP ${status}.`;
    }

    return error instanceof Error
        ? error.message
        : 'Không thể xử lý yêu cầu.';
}

export default function ClosedRevenuePage() {
    const now = new Date();

    const { isAdmin } = useDemoRole();

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

    const [confirmOpened, setConfirmOpened] = useState(false);
    const [closing, setClosing] = useState(false);

    const [closeError, setCloseError] = useState<string | null>(null);
    const [closeSuccess, setCloseSuccess] = useState<string | null>(null);

    const validYear =
        yearInput !== '' &&
        Number.isInteger(Number(yearInput)) &&
        Number(yearInput) >= 1 &&
        Number(yearInput) <= 9999;

    // Tải báo cáo đã chốt
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

    // Tra cứu báo cáo
    function handleSearch() {
        if (!validYear) return;

        setCloseError(null);
        setCloseSuccess(null);

        setPeriod({
            year: Number(yearInput),
            month: Number(month),
        });

        setRefreshKey((value) => value + 1);
    }

    // Admin xác nhận chốt báo cáo
    async function handleConfirmClose() {
        if (!isAdmin || closing || !notFound) return;

        setClosing(true);
        setCloseError(null);
        setCloseSuccess(null);

        try {
            await closeMonthlyReport(
                period.year,
                period.month
            );

            setConfirmOpened(false);

            setCloseSuccess(
                `Đã chốt báo cáo tháng ${period.month}/${period.year} thành công.`
            );

            // Tải lại snapshot vừa được tạo
            setRefreshKey((value) => value + 1);
        } catch (err: unknown) {
            setConfirmOpened(false);

            if (
                axios.isAxiosError(err) &&
                err.response?.status === 409
            ) {
                setCloseError(
                    'Kỳ báo cáo đã tồn tại hoặc đã được chốt trước đó. Hệ thống đang kiểm tra lại dữ liệu.'
                );

                setRefreshKey((value) => value + 1);
            } else {
                setCloseError(getErrorMessage(err));
            }
        } finally {
            setClosing(false);
        }
    }

    return (
        <Stack gap="lg">
            {/* Tiêu đề */}
            <Box>
                <Title order={2} className={styles.pageTitle}>
                    Báo cáo doanh thu đã chốt
                </Title>

                <Text size="sm" c="dimmed" mt={4}>
                    Tra cứu dữ liệu snapshot được lưu trong hệ thống
                    theo từng tháng.
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
                        disabled={!validYear || closing}
                        loading={loading}
                    >
                        Xem báo cáo
                    </Button>
                </Group>
            </Paper>

            {/* Thông báo thao tác */}
            {closeSuccess && (
                <Alert color="green" title="Chốt báo cáo thành công">
                    {closeSuccess}
                </Alert>
            )}

            {closeError && (
                <Alert color="red" title="Không thể chốt báo cáo">
                    {closeError}
                </Alert>
            )}

            {/* Loading */}
            {loading && (
                <Paper withBorder radius="sm" p="xl">
                    <Group justify="center">
                        <Loader size="sm" />
                        <Text size="sm" c="dimmed">
                            Đang tải báo cáo...
                        </Text>
                    </Group>
                </Paper>
            )}

            {/* Lỗi GET */}
            {error && !loading && (
                <Alert color="red" title="Không thể tải báo cáo">
                    {error}
                </Alert>
            )}

            {/* Chưa có snapshot */}
            {notFound && !loading && !error && (
                <Paper withBorder radius="sm" p="lg">
                    <Stack gap="md">
                        <Group justify="space-between">
                            <Box>
                                <Text fw={600}>
                                    Báo cáo tháng {period.month}/{period.year}
                                </Text>

                                <Text size="sm" c="dimmed" mt={4}>
                                    Chưa tìm thấy báo cáo đã chốt cho kỳ này.
                                </Text>
                            </Box>

                            <Badge color="yellow" variant="light">
                                Chưa chốt
                            </Badge>
                        </Group>

                        {isAdmin ? (
                            <Alert color="blue" title="Quyền Admin">
                                Bạn có thể thực hiện chốt báo cáo cho kỳ
                                này sau khi kiểm tra dữ liệu nghiệp vụ.
                            </Alert>
                        ) : (
                            <Alert color="gray" title="Chưa có báo cáo">
                                Bạn có quyền xem báo cáo nhưng không có
                                quyền thực hiện thao tác chốt.
                            </Alert>
                        )}

                        {isAdmin && (
                            <Group justify="flex-end">
                                <Button
                                    color="blue"
                                    onClick={() => setConfirmOpened(true)}
                                    disabled={closing}
                                >
                                    Chốt báo cáo
                                </Button>
                            </Group>
                        )}
                    </Stack>
                </Paper>
            )}

            {/* Snapshot đã tồn tại */}
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
                                    Thời gian chốt
                                </Text>

                                <Text size="sm" fw={500}>
                                    {formatDate(data.generatedAt)}
                                </Text>
                            </Group>
                        </Stack>
                    </Paper>

                    {isAdmin && (
                        <Alert color="green" title="Báo cáo đã được chốt">
                            Kỳ báo cáo này đã có snapshot. Không thể chốt
                            lại cùng kỳ thông qua giao diện.
                        </Alert>
                    )}
                </Stack>
            )}

            {/* Modal xác nhận */}
            <Modal
                opened={confirmOpened}
                onClose={() => {
                    if (!closing) {
                        setConfirmOpened(false);
                    }
                }}
                title="Xác nhận chốt báo cáo"
                centered
                closeOnClickOutside={!closing}
                closeOnEscape={!closing}
            >
                <Stack gap="md">
                    <Text size="sm">
                        Bạn có chắc chắn muốn chốt báo cáo doanh thu
                        tháng <strong>{period.month}/{period.year}</strong>?
                    </Text>

                    <Alert color="yellow" title="Lưu ý">
                        Sau khi chốt, hệ thống sẽ lưu snapshot báo cáo
                        vào database. Hãy kiểm tra kỹ kỳ báo cáo trước
                        khi xác nhận.
                    </Alert>

                    <Group justify="flex-end">
                        <Button
                            variant="default"
                            onClick={() => setConfirmOpened(false)}
                            disabled={closing}
                        >
                            Hủy
                        </Button>

                        <Button
                            color="blue"
                            onClick={handleConfirmClose}
                            loading={closing}
                        >
                            Xác nhận chốt
                        </Button>
                    </Group>
                </Stack>
            </Modal>
        </Stack>
    );
}
