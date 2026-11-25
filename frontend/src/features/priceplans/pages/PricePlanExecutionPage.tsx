
import { useState } from 'react';
import axios from 'axios';
import { useNavigate, useSearchParams } from 'react-router-dom';

import {
    Alert,
    Badge,
    Box,
    Button,
    Checkbox,
    Divider,
    Group,
    Modal,
    NumberInput,
    Paper,
    SimpleGrid,
    Stack,
    Text,
    Title,
} from '@mantine/core';

import {
    applyPricePlan,
    endPricePlan,
} from '../api/pricePlanExecutionApi';

import type {
    PricePlanExecutionAction,
    PricePlanExecutionResponse,
} from '../types/PricePlanExecution';

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
});

function formatMoney(value: number | string): string {
    const amount = Number(value);

    return Number.isFinite(amount)
        ? moneyFormatter.format(amount)
        : String(value);
}

function formatDate(value: string | null): string {
    if (!value) return 'Không giới hạn';

    const parts = value.split('-');

    if (parts.length !== 3) return value;

    return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

function formatDateTime(value: string | null): string {
    if (!value) return 'Chưa áp dụng';

    return value.replace('T', ' ').slice(0, 19);
}

function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const data = error.response?.data as
            { message?: string } | undefined;

        if (data?.message) return data.message;

        if (!error.response) {
            return 'Không kết nối được Backend Spring Boot.';
        }

        return `Backend trả về HTTP ${error.response.status}.`;
    }

    return error instanceof Error
        ? error.message
        : 'Không thể thực hiện kế hoạch giá.';
}

function getStatusColor(status: string): string {
    switch (status) {
        case 'Approved':
            return 'green';
        case 'Applied':
            return 'blue';
        case 'Ended':
            return 'gray';
        case 'Rejected':
            return 'red';
        default:
            return 'yellow';
    }
}

export default function PricePlanExecutionPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const [planIdInput, setPlanIdInput] =
        useState<number | string>(
            searchParams.get('planId') ?? ''
        );

    const [selectedAction, setSelectedAction] =
        useState<PricePlanExecutionAction | null>(null);

    const [confirmed, setConfirmed] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [result, setResult] =
        useState<PricePlanExecutionResponse | null>(null);

    const [successMessage, setSuccessMessage] =
        useState<string | null>(null);

    const planId = Number(planIdInput);

    const validPlanId =
        planIdInput !== '' &&
        Number.isSafeInteger(planId) &&
        planId > 0;

    function openConfirmation(
        action: PricePlanExecutionAction
    ) {
        if (!validPlanId || loading) return;

        setSelectedAction(action);
        setConfirmed(false);
        setError(null);
        setSuccessMessage(null);
        setResult(null);
    }

    function closeConfirmation() {
        if (loading) return;

        setSelectedAction(null);
        setConfirmed(false);
        setError(null);
    }

    async function executeAction() {
        if (
            !selectedAction ||
            !validPlanId ||
            !confirmed ||
            loading
        ) {
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const response =
                selectedAction === 'apply'
                    ? await applyPricePlan(planId)
                    : await endPricePlan(planId);

            setResult(response);

            setSuccessMessage(
                selectedAction === 'apply'
                    ? `Kế hoạch #${response.planId} đã được áp dụng thành công.`
                    : `Kế hoạch #${response.planId} đã được kết thúc thành công.`
            );

            setSelectedAction(null);
            setConfirmed(false);
        } catch (err: unknown) {
            setError(getErrorMessage(err));
        } finally {
            setLoading(false);
        }
    }

    const isApplying = selectedAction === 'apply';

    return (
        <Stack gap="lg">
            {/* Header */}
            <Group justify="space-between" align="start">
                <Box>
                    <Title order={2}>
                        Thực thi kế hoạch điều chỉnh giá
                    </Title>

                    <Text size="sm" c="dimmed" mt={5}>
                        US-15: Admin áp dụng hoặc kết thúc
                        kế hoạch giá đã được phê duyệt.
                    </Text>
                </Box>

                <Badge color="red" variant="light">
                    ADMIN
                </Badge>
            </Group>

            <Group>
                <Button
                    variant="subtle"
                    size="sm"
                    onClick={() => navigate(-1)}
                >
                    Quay lại trang trước
                </Button>
            </Group>

            {/* Cảnh báo */}
            <Alert
                color="yellow"
                title="Lưu ý: Thao tác thay đổi giá phòng thực tế"
            >
                Khi áp dụng, hệ thống sẽ cập nhật giá của
                tất cả phòng thuộc loại phòng tương ứng.
                Khi kết thúc, hệ thống khôi phục giá cũ.
                Hãy kiểm tra đúng ID kế hoạch trước khi thực hiện.
            </Alert>

            {/* Nhập ID kế hoạch */}
            <Paper withBorder radius="md" p="lg">
                <Stack gap="md">
                    <Box>
                        <Title order={4}>
                            Chọn kế hoạch cần thực thi
                        </Title>

                        <Text size="sm" c="dimmed" mt={5}>
                            Nhập ID kế hoạch từ danh sách phê duyệt
                            US-10. Backend sẽ kiểm tra trạng thái
                            và điều kiện thực thi.
                        </Text>
                    </Box>

                    <NumberInput
                        label="ID kế hoạch giá"
                        placeholder="Ví dụ: 10"
                        description="Chỉ nhập số nguyên dương."
                        value={planIdInput}
                        onChange={(value) => {
                            setPlanIdInput(value);
                            setResult(null);
                            setSuccessMessage(null);
                            setError(null);
                        }}
                        min={1}
                        allowNegative={false}
                        allowDecimal={false}
                        w={280}
                    />

                    <Divider />

                    <SimpleGrid cols={{ base: 1, sm: 2 }}>
                        <Paper
                            withBorder
                            radius="md"
                            p="md"
                        >
                            <Stack gap="sm">
                                <Badge
                                    color="green"
                                    variant="light"
                                    w="fit-content"
                                >
                                    US-15A
                                </Badge>

                                <Title order={4}>
                                    Áp dụng kế hoạch
                                </Title>

                                <Text size="sm" c="dimmed">
                                    Chuyển Approved thành Applied
                                    và thay giá cũ bằng giá mới
                                    trong bảng rooms.
                                </Text>

                                <Button
                                    color="green"
                                    disabled={!validPlanId || loading}
                                    onClick={() =>
                                        openConfirmation('apply')
                                    }
                                >
                                    Áp dụng kế hoạch
                                </Button>
                            </Stack>
                        </Paper>

                        <Paper
                            withBorder
                            radius="md"
                            p="md"
                        >
                            <Stack gap="sm">
                                <Badge
                                    color="blue"
                                    variant="light"
                                    w="fit-content"
                                >
                                    US-15B
                                </Badge>

                                <Title order={4}>
                                    Kết thúc kế hoạch
                                </Title>

                                <Text size="sm" c="dimmed">
                                    Chuyển Applied thành Ended
                                    và khôi phục giá phòng
                                    về giá ban đầu.
                                </Text>

                                <Button
                                    color="blue"
                                    disabled={!validPlanId || loading}
                                    onClick={() =>
                                        openConfirmation('end')
                                    }
                                >
                                    Kết thúc kế hoạch
                                </Button>
                            </Stack>
                        </Paper>
                    </SimpleGrid>
                </Stack>
            </Paper>

            {/* Thông báo thành công */}
            {successMessage && result && (
                <Alert
                    color="green"
                    title="Thao tác thành công"
                >
                    {successMessage}
                </Alert>
            )}

            {/* Kết quả trả về */}
            {result && (
                <Paper withBorder radius="md" p="lg">
                    <Stack gap="md">
                        <Group justify="space-between">
                            <Title order={4}>
                                Kết quả thực thi
                            </Title>

                            <Badge
                                color={getStatusColor(result.status)}
                                variant="light"
                            >
                                {result.status}
                            </Badge>
                        </Group>

                        <SimpleGrid
                            cols={{ base: 1, sm: 2 }}
                            spacing="lg"
                        >
                            <Box>
                                <Text size="xs" c="dimmed">
                                    ID kế hoạch
                                </Text>

                                <Text fw={600}>
                                    #{result.planId}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    ID loại phòng
                                </Text>

                                <Text fw={600}>
                                    #{result.categoryId}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Giá cũ
                                </Text>

                                <Text fw={600}>
                                    {formatMoney(result.oldPrice)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Giá mới
                                </Text>

                                <Text fw={600}>
                                    {formatMoney(result.newPrice)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Bắt đầu hiệu lực
                                </Text>

                                <Text fw={500}>
                                    {formatDate(result.effectiveFrom)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Kết thúc hiệu lực
                                </Text>

                                <Text fw={500}>
                                    {formatDate(result.effectiveTo)}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Trạng thái sau thực thi
                                </Text>

                                <Text fw={600}>
                                    {result.status}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Thời điểm áp dụng
                                </Text>

                                <Text fw={500}>
                                    {formatDateTime(result.appliedAt)}
                                </Text>
                            </Box>
                        </SimpleGrid>

                        <Text size="xs" c="dimmed">
                            Thông tin trên được trả trực tiếp
                            từ Backend sau khi transaction
                            hoàn tất.
                        </Text>
                    </Stack>
                </Paper>
            )}

            {/* Hộp thoại xác nhận */}
            <Modal
                opened={selectedAction !== null}
                onClose={closeConfirmation}
                title={
                    isApplying
                        ? 'Xác nhận áp dụng kế hoạch'
                        : 'Xác nhận kết thúc kế hoạch'
                }
                centered
                closeOnClickOutside={!loading}
                closeOnEscape={!loading}
                withCloseButton={!loading}
            >
                <Stack gap="md">
                    <Text size="sm">
                        Bạn đang thao tác với kế hoạch{' '}
                        <strong>#{planId}</strong>.
                    </Text>

                    <Alert
                        color={isApplying ? 'yellow' : 'blue'}
                    >
                        {isApplying
                            ? 'Hệ thống sẽ đổi giá thật của các phòng từ oldPrice sang newPrice.'
                            : 'Hệ thống sẽ khôi phục giá thật của các phòng từ newPrice về oldPrice.'}
                    </Alert>

                    <Checkbox
                        checked={confirmed}
                        onChange={(event) =>
                            setConfirmed(
                                event.currentTarget.checked
                            )
                        }
                        label="Tôi đã kiểm tra ID và hiểu rằng thao tác sẽ thay đổi giá phòng trong database."
                    />

                    {error && (
                        <Alert
                            color="red"
                            title="Không thể thực hiện"
                        >
                            {error}
                        </Alert>
                    )}

                    <Group justify="flex-end">
                        <Button
                            variant="default"
                            onClick={closeConfirmation}
                            disabled={loading}
                        >
                            Quay lại
                        </Button>

                        <Button
                            color={isApplying ? 'green' : 'blue'}
                            loading={loading}
                            disabled={!confirmed}
                            onClick={executeAction}
                        >
                            Xác nhận thực hiện
                        </Button>
                    </Group>
                </Stack>
            </Modal>
        </Stack>
    );
}
