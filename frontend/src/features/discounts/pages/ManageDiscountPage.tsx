
import { useEffect, useState } from 'react';
import axios from 'axios';

import {
    Alert,
    Badge,
    Box,
    Button,
    Divider,
    Group,
    Loader,
    Modal,
    NumberInput,
    Paper,
    Select,
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

import {
    checkDiscountEligibility,
    disableDiscount,
    extendDiscount,
} from '../api/discountManagementApi';

import type {
    DiscountEffectivenessResponse,
} from '../types/DiscountEffectiveness';

import type {
    DiscountEligibilityResponse,
} from '../types/DiscountManagement';

import styles from './ManageDiscountPage.module.css';


type ActionType = 'disable' | 'extend' | null;

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
});

const numberFormatter = new Intl.NumberFormat('vi-VN');

function formatMoney(value: number | string) {
    const amount = Number(value);

    return Number.isFinite(amount)
        ? moneyFormatter.format(amount)
        : 'Không hợp lệ';
}

function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        if (!error.response) {
            return 'Không kết nối được Spring Boot. Hãy kiểm tra backend.';
        }

        const body = error.response.data as
            | { message?: string }
            | undefined;

        if (error.response.status === 400) {
            return body?.message ?? 'Dữ liệu gửi lên không hợp lệ.';
        }

        if (error.response.status === 404) {
            return body?.message ?? 'Không tìm thấy mã chiết khấu.';
        }

        if (error.response.status === 409) {
            return body?.message ?? 'Trạng thái mã đã thay đổi. Hãy tải lại dữ liệu.';
        }

        return body?.message ??
            `Backend trả về HTTP ${error.response.status}.`;
    }

    return error instanceof Error
        ? error.message
        : 'Đã xảy ra lỗi không xác định.';
}

function getStatusColor(status: string) {
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

function getStatusLabel(status: string) {
    switch (status.toLowerCase()) {
        case 'active':
            return 'Active';
        case 'expired':
            return 'Expired';
        case 'disabled':
            return 'Disabled';
        default:
            return status;
    }
}

export default function ManageDiscountPage() {
    const [discounts, setDiscounts] = useState<
        DiscountEffectivenessResponse[]
    >([]);

    const [selectedId, setSelectedId] = useState<string | null>(null);

    const [detail, setDetail] =
        useState<DiscountEligibilityResponse | null>(null);

    const [eligibility, setEligibility] =
        useState<DiscountEligibilityResponse | null>(null);

    const [orderAmount, setOrderAmount] =
        useState<number | string>('');

    const [newValidTo, setNewValidTo] = useState('');

    const [loadingList, setLoadingList] = useState(true);
    const [loadingDetail, setLoadingDetail] = useState(false);
    const [checking, setChecking] = useState(false);
    const [saving, setSaving] = useState(false);

    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [actionError, setActionError] = useState<string | null>(null);

    const [actionModal, setActionModal] =
        useState<ActionType>(null);

    const [reloadKey, setReloadKey] = useState(0);

    // 1. Lấy danh sách mã từ API US-06.
    useEffect(() => {
        const controller = new AbortController();

        setLoadingList(true);
        setError(null);

        getDiscountEffectiveness(
            undefined,
            undefined,
            controller.signal
        )
            .then((result) => {
                if (controller.signal.aborted) return;

                setDiscounts(result);

                setSelectedId((current) => {
                    if (
                        current &&
                        result.some(
                            (item) => String(item.discountId) === current
                        )
                    ) {
                        return current;
                    }

                    return result.length > 0
                        ? String(result[0].discountId)
                        : null;
                });
            })
            .catch((err: unknown) => {
                if (!controller.signal.aborted) {
                    setError(getErrorMessage(err));
                    setDiscounts([]);
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoadingList(false);
                }
            });

        return () => controller.abort();
    }, [reloadKey]);

    // 2. Lấy thông tin chi tiết mã khi chọn mã.
    // orderAmount = 0 chỉ dùng để đọc thông tin mã.
    useEffect(() => {
        if (!selectedId) {
            setDetail(null);
            setEligibility(null);
            return;
        }

        const controller = new AbortController();

        setLoadingDetail(true);
        setDetail(null);
        setEligibility(null);
        setError(null);

        checkDiscountEligibility(
            Number(selectedId),
            0,
            controller.signal
        )
            .then((result) => {
                if (!controller.signal.aborted) {
                    setDetail(result);
                }
            })
            .catch((err: unknown) => {
                if (!controller.signal.aborted) {
                    setError(getErrorMessage(err));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setLoadingDetail(false);
                }
            });

        return () => controller.abort();
    }, [selectedId, reloadKey]);



    const discountOptions = discounts.map((item) => ({
        value: String(item.discountId),
        label: `${item.code} (ID: ${item.discountId})`,
    }));

    // 3. Kiểm tra mã với giá trị đơn hàng thực tế.
    async function handleCheckEligibility() {
        if (!selectedId) return;

        if (
            orderAmount === '' ||
            !Number.isFinite(Number(orderAmount)) ||
            Number(orderAmount) < 0
        ) {
            setError('Giá trị đơn hàng phải là số không âm.');
            return;
        }

        setChecking(true);
        setError(null);
        setSuccess(null);
        setEligibility(null);

        try {
            const result = await checkDiscountEligibility(
                Number(selectedId),
                Number(orderAmount)
            );

            setEligibility(result);
            setDetail(result);
        } catch (err: unknown) {
            setError(getErrorMessage(err));
        } finally {
            setChecking(false);
        }
    }

    // 4. Mở hộp thoại xác nhận thao tác.
    function openActionModal(action: ActionType) {
        setActionError(null);
        setError(null);
        setSuccess(null);
        setActionModal(action);
    }

    function closeActionModal() {
        if (saving) return;

        setActionModal(null);
        setActionError(null);
    }

    // 5. Xác nhận vô hiệu hóa hoặc gia hạn.
    async function handleConfirmAction() {
        if (!selectedId || !detail || !actionModal || saving) {
            return;
        }

        const id = Number(selectedId);

        if (actionModal === 'extend') {
            if (!newValidTo) {
                setActionError('Vui lòng chọn ngày hết hạn mới.');
                return;
            }

            if (newValidTo <= detail.validTo) {
                setActionError(
                    'Ngày hết hạn mới phải sau ngày hết hạn hiện tại.'
                );
                return;
            }
        }

        setSaving(true);
        setActionError(null);
        setError(null);
        setSuccess(null);

        try {
            if (actionModal === 'disable') {
                const result = await disableDiscount(id);

                setSuccess(
                    `Đã vô hiệu hóa mã ${result.code} thành công.`
                );
            } else {
                const result = await extendDiscount(id, {
                    newValidTo,
                });

                setSuccess(
                    `Đã gia hạn mã ${result.code} đến ${result.validTo}.`
                );

                if (result.storedStatus === 'Disabled') {
                    setSuccess(
                        `Đã gia hạn mã ${result.code}, nhưng mã vẫn ở trạng thái Disabled.`
                    );
                }
            }

            setActionModal(null);
            setNewValidTo('');
            setEligibility(null);

            // Tải lại danh sách và thông tin chi tiết.
            setReloadKey((prev) => prev + 1);
        } catch (err: unknown) {
            setActionError(getErrorMessage(err));
        } finally {
            setSaving(false);
        }
    }

    return (
        <Stack gap="lg">
            {/* Tiêu đề */}
            <Box>
                <Title order={2} className={styles.pageTitle}>
                    Quản lý mã chiết khấu
                </Title>

                <Text size="sm" c="dimmed" mt={4}>
                    Kiểm tra hiệu lực, gia hạn và vô hiệu hóa
                    mã chiết khấu trong hệ thống Home Rent.
                </Text>
            </Box>

            <DiscountNavigation />

            <Alert color="blue" title="US-07 – Quản lý mã">
                Các thao tác cập nhật sẽ ghi dữ liệu trực tiếp
                vào SQL Server thông qua Spring Boot local-demo.
            </Alert>

            {/* Thông báo */}
            {success && (
                <Alert color="green" title="Thao tác thành công">
                    {success}
                </Alert>
            )}

            {error && (
                <Alert color="red" title="Không thể thực hiện">
                    {error}
                </Alert>
            )}

            {/* Danh sách mã */}
            <Paper withBorder radius="sm" p="lg">
                <Group justify="space-between" mb="md">
                    <Box>
                        <Text fw={600}>
                            Danh sách mã chiết khấu
                        </Text>
                        <Text size="xs" c="dimmed" mt={4}>
                            Dữ liệu lấy từ API thống kê US-06.
                        </Text>
                    </Box>

                    <Button
                        variant="default"
                        size="sm"
                        onClick={() => setReloadKey((prev) => prev + 1)}
                        loading={loadingList}
                    >
                        Tải lại
                    </Button>
                </Group>

                {loadingList ? (
                    <Group justify="center" p="xl">
                        <Loader size="sm" />
                        <Text size="sm" c="dimmed">
                            Đang tải danh sách mã...
                        </Text>
                    </Group>
                ) : discounts.length === 0 ? (
                    <Text size="sm" c="dimmed">
                        Chưa có mã chiết khấu trong hệ thống.
                    </Text>
                ) : (
                    <Table.ScrollContainer minWidth={650}>
                        <Table striped highlightOnHover>
                            <Table.Thead>
                                <Table.Tr>
                                    <Table.Th>ID</Table.Th>
                                    <Table.Th>Mã</Table.Th>
                                    <Table.Th>Trạng thái thống kê</Table.Th>
                                    <Table.Th ta="right">Lượt dùng</Table.Th>
                                    <Table.Th ta="right">Doanh thu</Table.Th>
                                    <Table.Th></Table.Th>
                                </Table.Tr>
                            </Table.Thead>

                            <Table.Tbody>
                                {discounts.map((item) => (
                                    <Table.Tr key={item.discountId}>
                                        <Table.Td>
                                            {item.discountId}
                                        </Table.Td>

                                        <Table.Td>
                                            <Text fw={600} size="sm">
                                                {item.code}
                                            </Text>
                                        </Table.Td>

                                        <Table.Td>
                                            <Badge
                                                color={getStatusColor(item.effectiveStatus)}
                                                variant="light"
                                            >
                                                {getStatusLabel(item.effectiveStatus)}
                                            </Badge>
                                        </Table.Td>

                                        <Table.Td ta="right">
                                            {numberFormatter.format(item.uses)}
                                        </Table.Td>

                                        <Table.Td ta="right">
                                            {formatMoney(item.netRevenue)}
                                        </Table.Td>

                                        <Table.Td>
                                            <Button
                                                size="xs"
                                                variant={
                                                    selectedId === String(item.discountId)
                                                        ? 'light'
                                                        : 'default'
                                                }
                                                onClick={() => {
                                                    setSelectedId(String(item.discountId));
                                                    setOrderAmount('');
                                                    setSuccess(null);
                                                }}
                                            >
                                                {selectedId === String(item.discountId)
                                                    ? 'Đang chọn'
                                                    : 'Quản lý'}
                                            </Button>
                                        </Table.Td>
                                    </Table.Tr>
                                ))}
                            </Table.Tbody>
                        </Table>
                    </Table.ScrollContainer>
                )}
            </Paper>

            {/* Khu vực quản lý mã đã chọn */}
            {selectedId && (
                <Paper withBorder radius="sm" p="lg">
                    <Stack gap="lg">
                        <Box>
                            <Text fw={600}>
                                Thông tin mã chiết khấu
                            </Text>
                            <Text size="xs" c="dimmed" mt={4}>
                                Chọn mã trong danh sách để xem và quản lý.
                            </Text>
                        </Box>

                        <Select
                            label="Mã chiết khấu"
                            placeholder="Chọn mã"
                            data={discountOptions}
                            value={selectedId}
                            onChange={(value) => {
                                setSelectedId(value);
                                setOrderAmount('');
                                setSuccess(null);
                            }}
                            searchable
                            allowDeselect={false}
                            disabled={loadingList || saving}
                        />

                        {loadingDetail ? (
                            <Group justify="center" p="lg">
                                <Loader size="sm" />
                                <Text size="sm" c="dimmed">
                                    Đang tải thông tin mã...
                                </Text>
                            </Group>
                        ) : detail ? (
                            <>
                                <Divider />

                                <SimpleGrid
                                    cols={{ base: 1, sm: 2, lg: 3 }}
                                    spacing="lg"
                                >
                                    <Box>
                                        <Text size="xs" c="dimmed">
                                            Trạng thái lưu trong database
                                        </Text>
                                        <Badge
                                            mt={6}
                                            color={getStatusColor(detail.storedStatus)}
                                        >
                                            {getStatusLabel(detail.storedStatus)}
                                        </Badge>
                                    </Box>

                                    <Box>
                                        <Text size="xs" c="dimmed">
                                            Trạng thái hiệu lực
                                        </Text>
                                        <Badge
                                            mt={6}
                                            color={getStatusColor(detail.effectiveStatus)}
                                        >
                                            {getStatusLabel(detail.effectiveStatus)}
                                        </Badge>
                                    </Box>

                                    <Box>
                                        <Text size="xs" c="dimmed">
                                            Giá trị đơn tối thiểu
                                        </Text>
                                        <Text fw={600} mt={6}>
                                            {formatMoney(detail.minAmount)}
                                        </Text>
                                    </Box>

                                    <Box>
                                        <Text size="xs" c="dimmed">
                                            Ngày bắt đầu
                                        </Text>
                                        <Text fw={600} mt={6}>
                                            {detail.validFrom}
                                        </Text>
                                    </Box>

                                    <Box>
                                        <Text size="xs" c="dimmed">
                                            Ngày hết hạn
                                        </Text>
                                        <Text fw={600} mt={6}>
                                            {detail.validTo}
                                        </Text>
                                    </Box>

                                    <Box>
                                        <Text size="xs" c="dimmed">
                                            Lượt đã sử dụng
                                        </Text>
                                        <Text fw={600} mt={6}>
                                            {numberFormatter.format(detail.uses)}
                                        </Text>
                                    </Box>

                                    <Box>
                                        <Text size="xs" c="dimmed">
                                            Giới hạn sử dụng
                                        </Text>
                                        <Text fw={600} mt={6}>
                                            {detail.usageLimit === null
                                                ? 'Không giới hạn'
                                                : numberFormatter.format(detail.usageLimit)}
                                        </Text>
                                    </Box>

                                    <Box>
                                        <Text size="xs" c="dimmed">
                                            Lượt còn lại
                                        </Text>
                                        <Text fw={600} mt={6}>
                                            {detail.remainingUses === null
                                                ? 'Không giới hạn'
                                                : numberFormatter.format(detail.remainingUses)}
                                        </Text>
                                    </Box>
                                </SimpleGrid>

                                <Divider />

                                {/* Kiểm tra hiệu lực */}
                                <Box>
                                    <Text fw={600} mb={4}>
                                        Kiểm tra khả năng áp dụng
                                    </Text>

                                    <Text size="xs" c="dimmed" mb="md">
                                        Nhập giá trị đơn hàng để kiểm tra
                                        điều kiện sử dụng mã.
                                    </Text>

                                    <Group align="end" wrap="wrap">
                                        <NumberInput
                                            label="Giá trị đơn hàng (VND)"
                                            placeholder="VD: 1500000"
                                            value={orderAmount}
                                            onChange={setOrderAmount}
                                            min={0}
                                            decimalScale={2}
                                            allowNegative={false}
                                            disabled={checking || saving}
                                            w={{ base: '100%', sm: 260 }}
                                        />

                                        <Button
                                            onClick={handleCheckEligibility}
                                            loading={checking}
                                            disabled={saving}
                                        >
                                            Kiểm tra hiệu lực
                                        </Button>
                                    </Group>
                                </Box>

                                {eligibility && (
                                    <Alert
                                        color={eligibility.canApply ? 'green' : 'yellow'}
                                        title={
                                            eligibility.canApply
                                                ? 'Mã có thể áp dụng'
                                                : 'Mã không thể áp dụng'
                                        }
                                    >
                                        <Stack gap="xs">
                                            <Text size="sm">
                                                {eligibility.reason}
                                            </Text>

                                            <Group justify="space-between">
                                                <Text size="xs" c="dimmed">
                                                    Giá trị đơn hàng
                                                </Text>
                                                <Text size="sm" fw={600}>
                                                    {formatMoney(eligibility.orderAmount)}
                                                </Text>
                                            </Group>

                                            <Group justify="space-between">
                                                <Text size="xs" c="dimmed">
                                                    Giá trị tối thiểu
                                                </Text>
                                                <Text size="sm" fw={600}>
                                                    {formatMoney(eligibility.minAmount)}
                                                </Text>
                                            </Group>
                                        </Stack>
                                    </Alert>
                                )}

                                <Divider />

                                {/* Thao tác quản lý */}
                                <Box>
                                    <Text fw={600} mb={4}>
                                        Thao tác quản lý
                                    </Text>

                                    <Text size="xs" c="dimmed" mb="md">
                                        Gia hạn không tự động kích hoạt lại
                                        mã đã bị vô hiệu hóa.
                                    </Text>

                                    <Group wrap="wrap">
                                        <Button
                                            variant="default"
                                            onClick={() => openActionModal('extend')}
                                            disabled={saving}
                                        >
                                            Gia hạn mã
                                        </Button>

                                        <Button
                                            color="red"
                                            variant="light"
                                            onClick={() => openActionModal('disable')}
                                            disabled={
                                                saving ||
                                                detail.storedStatus === 'Disabled'
                                            }
                                        >
                                            Vô hiệu hóa mã
                                        </Button>
                                    </Group>
                                </Box>
                            </>
                        ) : (
                            <Text size="sm" c="dimmed">
                                Không có thông tin chi tiết mã chiết khấu.
                            </Text>
                        )}
                    </Stack>
                </Paper>
            )}

            {/* Modal xác nhận */}
            <Modal
                opened={actionModal !== null}
                onClose={closeActionModal}
                centered
                title={
                    actionModal === 'disable'
                        ? 'Xác nhận vô hiệu hóa mã'
                        : 'Gia hạn mã chiết khấu'
                }
                closeOnClickOutside={!saving}
                closeOnEscape={!saving}
                withCloseButton={!saving}
            >
                <Stack gap="md">
                    {actionModal === 'disable' ? (
                        <>
                            <Text size="sm">
                                Bạn có chắc chắn muốn vô hiệu hóa mã
                                {' '}<Text span fw={700}>{detail?.code}</Text>?
                            </Text>

                            <Alert color="yellow">
                                Sau khi vô hiệu hóa, mã sẽ không thể
                                áp dụng cho các đơn hàng mới.
                            </Alert>
                        </>
                    ) : (
                        <>
                            <Text size="sm">
                                Ngày hết hạn hiện tại:
                                {' '}
                                <Text span fw={600}>
                                    {detail?.validTo}
                                </Text>
                            </Text>

                            <TextInput
                                label="Ngày hết hạn mới"
                                type="date"
                                value={newValidTo}
                                onChange={(event) => {
                                    const value = event.currentTarget.value;
                                    setNewValidTo(value);
                                    setActionError(null);
                                }}
                                min={
                                    detail
                                        ? (() => {
                                            const date = new Date(
                                                `${detail.validTo}T00:00:00`
                                            );
                                            date.setDate(date.getDate() + 1);

                                            const year = date.getFullYear();
                                            const month = String(
                                                date.getMonth() + 1
                                            ).padStart(2, '0');
                                            const day = String(
                                                date.getDate()
                                            ).padStart(2, '0');

                                            return `${year}-${month}-${day}`;
                                        })()
                                        : undefined
                                }
                                required
                            />

                            {detail?.storedStatus === 'Disabled' && (
                                <Alert color="yellow">
                                    Mã đang Disabled. Gia hạn chỉ thay đổi
                                    ngày hết hạn, không kích hoạt lại mã.
                                </Alert>
                            )}
                        </>
                    )}

                    {actionError && (
                        <Alert color="red">
                            {actionError}
                        </Alert>
                    )}

                    <Group justify="flex-end">
                        <Button
                            variant="default"
                            onClick={closeActionModal}
                            disabled={saving}
                        >
                            Hủy
                        </Button>

                        <Button
                            color={
                                actionModal === 'disable' ? 'red' : 'blue'
                            }
                            onClick={handleConfirmAction}
                            loading={saving}
                        >
                            {actionModal === 'disable'
                                ? 'Xác nhận vô hiệu hóa'
                                : 'Xác nhận gia hạn'}
                        </Button>
                    </Group>
                </Stack>
            </Modal>
        </Stack>
    );
}
