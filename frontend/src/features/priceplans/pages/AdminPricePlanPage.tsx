
import { useState } from 'react';
import axios from 'axios';

import {
    Alert,
    Badge,
    Box,
    Button,
    Divider,
    Group,
    Modal,
    NumberInput,
    Paper,
    Stack,
    Text,
    Textarea,
    Title,
} from '@mantine/core';

import { reviewPricePlan } from '../api/pricePlanReviewApi';
import { Link } from 'react-router-dom';

import type {
    ReviewPricePlanResponse,
} from '../types/PricePlanReview';

type ReviewAction = 'approve' | 'reject';

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
                return 'Thông tin xét duyệt không hợp lệ.';
            case 403:
                return 'Tài khoản Admin demo không có quyền thực hiện thao tác này.';
            case 404:
                return 'Không tìm thấy kế hoạch hoặc API xét duyệt.';
            case 409:
                return 'Kế hoạch không còn ở trạng thái Draft hoặc có xung đột nghiệp vụ.';
            default:
                return `Backend trả về HTTP ${error.response.status}.`;
        }
    }

    return error instanceof Error
        ? error.message
        : 'Không thể xét duyệt kế hoạch.';
}

function formatDateTime(value: string | null): string {
    if (!value) return '—';

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleString('vi-VN');
}

export default function AdminPricePlanPage() {
    const [planId, setPlanId] = useState<number | string>('');

    const [pendingAction, setPendingAction] =
        useState<ReviewAction | null>(null);

    const [rejectReason, setRejectReason] = useState('');

    const [reasonError, setReasonError] =
        useState<string | null>(null);

    const [apiError, setApiError] =
        useState<string | null>(null);

    const [submitting, setSubmitting] = useState(false);

    const [result, setResult] =
        useState<ReviewPricePlanResponse | null>(null);

    const validPlanId = Number(planId);

    const isValidPlanId =
        planId !== '' &&
        Number.isSafeInteger(validPlanId) &&
        validPlanId > 0;

    const isFinished =
        result?.planId === validPlanId &&
        ['Approved', 'Rejected'].includes(result.status);

    function openReview(action: ReviewAction) {
        if (!isValidPlanId || submitting || isFinished) {
            return;
        }

        setPendingAction(action);
        setRejectReason('');
        setReasonError(null);
        setApiError(null);
    }

    function closeModal() {
        if (submitting) return;

        setPendingAction(null);
        setReasonError(null);
    }

    async function handleConfirm() {
        if (
            pendingAction === null ||
            !isValidPlanId ||
            submitting
        ) {
            return;
        }

        const approve = pendingAction === 'approve';
        const reason = rejectReason.trim();

        if (!approve && !reason) {
            setReasonError(
                'Bạn phải nhập lý do khi từ chối kế hoạch.'
            );
            return;
        }

        setReasonError(null);
        setApiError(null);
        setSubmitting(true);

        try {
            const response = await reviewPricePlan(
                validPlanId,
                {
                    approve,
                    rejectReason: approve ? null : reason,
                }
            );

            setResult(response);
            setPendingAction(null);
            setRejectReason('');
        } catch (error: unknown) {
            setApiError(getErrorMessage(error));
            setPendingAction(null);
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <Stack gap="lg">
            <Group justify="space-between" align="start">
                <Box>
                    <Title order={2}>
                        Quản lý kế hoạch giá
                    </Title>

                    <Text size="sm" c="dimmed" mt={5}>
                        US-10: Admin phê duyệt hoặc từ chối
                        kế hoạch điều chỉnh giá của Manager.
                    </Text>
                </Box>

                <Badge color="blue" variant="light">
                    ADMIN
                </Badge>
            </Group>

            <Alert color="blue" title="Quy tắc xét duyệt">
                Chỉ kế hoạch ở trạng thái Draft mới được
                xét duyệt. Phê duyệt không đồng nghĩa với
                việc thay đổi giá phòng ngay lập tức.
            </Alert>

            <Paper withBorder radius="sm" p="lg">
                <Stack gap="md">
                    <Box>
                        <Title order={4}>
                            Xét duyệt kế hoạch
                        </Title>

                        <Text size="sm" c="dimmed" mt={5}>
                            Nhập mã kế hoạch đã được Manager tạo.
                        </Text>
                    </Box>

                    <NumberInput
                        label="Mã kế hoạch (Plan ID)"
                        placeholder="Ví dụ: 10"
                        description="Bạn có thể dùng kế hoạch #10 vừa tạo ở US-09."
                        value={planId}
                        onChange={(value) => {
                            setPlanId(value);
                            setResult(null);
                            setApiError(null);
                        }}
                        min={1}
                        allowNegative={false}
                        allowDecimal={false}
                        required
                    />

                    <Alert color="yellow" title="Kiểm tra trước khi duyệt">
                        Phiên bản này chưa có API lấy danh sách
                        hoặc chi tiết kế hoạch. Hãy đối chiếu
                        đúng mã kế hoạch, giá đề xuất và ngày
                        hiệu lực trong SSMS trước khi xác nhận.
                    </Alert>

                    <Group justify="flex-end">
                        <Button
                            color="red"
                            variant="light"
                            disabled={
                                !isValidPlanId ||
                                submitting ||
                                isFinished
                            }
                            onClick={() => openReview('reject')}
                        >
                            Từ chối
                        </Button>

                        <Button
                            color="green"
                            disabled={
                                !isValidPlanId ||
                                submitting ||
                                isFinished
                            }
                            onClick={() => openReview('approve')}
                        >
                            Phê duyệt
                        </Button>
                    </Group>

                    {apiError && (
                        <Alert
                            color="red"
                            title="Xét duyệt thất bại"
                        >
                            {apiError}
                        </Alert>
                    )}
                </Stack>
            </Paper>

            {result && (
                <Paper withBorder radius="sm" p="lg">
                    <Stack gap="md">
                        <Group justify="space-between">
                            <Title order={4}>
                                Kết quả xét duyệt
                            </Title>

                            <Badge
                                color={
                                    result.status === 'Approved'
                                        ? 'green'
                                        : result.status === 'Rejected'
                                            ? 'red'
                                            : 'gray'
                                }
                                variant="light"
                            >
                                {result.status}
                            </Badge>
                        </Group>

                        <Alert
                            color={
                                result.status === 'Approved'
                                    ? 'green'
                                    : result.status === 'Rejected'
                                        ? 'yellow'
                                        : 'blue'
                            }
                            title="Backend đã xử lý yêu cầu"
                        >
                            Kết quả bên dưới được trả về từ
                            API xét duyệt kế hoạch.
                        </Alert>

                        <Divider />

                        <Group justify="space-between">
                            <Text c="dimmed" size="sm">
                                Mã kế hoạch
                            </Text>
                            <Text fw={600}>
                                #{result.planId}
                            </Text>
                        </Group>

                        <Group justify="space-between">
                            <Text c="dimmed" size="sm">
                                Tên kế hoạch
                            </Text>
                            <Text fw={600}>
                                {result.title}
                            </Text>
                        </Group>

                        <Group justify="space-between">
                            <Text c="dimmed" size="sm">
                                Loại phòng
                            </Text>
                            <Text>
                                ID {result.categoryId}
                            </Text>
                        </Group>

                        <Group justify="space-between">
                            <Text c="dimmed" size="sm">
                                Người tạo
                            </Text>
                            <Text>
                                User ID {result.createdBy}
                            </Text>
                        </Group>

                        <Group justify="space-between">
                            <Text c="dimmed" size="sm">
                                Ngày bắt đầu
                            </Text>
                            <Text>
                                {result.effectiveFrom}
                            </Text>
                        </Group>

                        <Group justify="space-between">
                            <Text c="dimmed" size="sm">
                                Ngày kết thúc
                            </Text>
                            <Text>
                                {result.effectiveTo ?? 'Không xác định'}
                            </Text>
                        </Group>

                        {result.approvedBy !== null && (
                            <Group justify="space-between">
                                <Text c="dimmed" size="sm">
                                    Người duyệt
                                </Text>
                                <Text>
                                    User ID {result.approvedBy}
                                </Text>
                            </Group>
                        )}

                        {result.approvedAt && (
                            <Group justify="space-between">
                                <Text c="dimmed" size="sm">
                                    Thời gian duyệt
                                </Text>
                                <Text>
                                    {formatDateTime(result.approvedAt)}
                                </Text>
                            </Group>
                        )}

                        {result.rejectReason && (
                            <Alert color="red" title="Lý do từ chối">
                                {result.rejectReason}
                            </Alert>
                        )}
                    </Stack>
                </Paper>
            )}

            <Modal
                opened={pendingAction !== null}
                onClose={closeModal}
                title={
                    pendingAction === 'approve'
                        ? 'Xác nhận phê duyệt'
                        : 'Xác nhận từ chối'
                }
                centered
                closeOnClickOutside={!submitting}
                closeOnEscape={!submitting}
                withCloseButton={!submitting}
            >
                <Stack gap="md">
                    <Text size="sm">
                        Bạn đang xét duyệt kế hoạch
                        {' '}
                        <strong>#{validPlanId}</strong>.
                    </Text>

                    {pendingAction === 'approve' ? (
                        <Alert color="yellow">
                            Sau khi phê duyệt, kế hoạch
                            chuyển sang Approved nhưng giá
                            phòng thực tế chưa thay đổi.
                        </Alert>
                    ) : (
                        <Textarea
                            label="Lý do từ chối"
                            placeholder="Giải thích vì sao kế hoạch không được chấp thuận..."
                            value={rejectReason}
                            onChange={(event) => {
                                setRejectReason(
                                    event.currentTarget.value
                                );
                                setReasonError(null);
                            }}
                            minRows={3}
                            required
                            error={reasonError}
                        />
                    )}

                    <Group justify="flex-end">
                        <Button
                            variant="default"
                            onClick={closeModal}
                            disabled={submitting}
                        >
                            Hủy
                        </Button>

                        <Button
                            color={
                                pendingAction === 'approve'
                                    ? 'green'
                                    : 'red'
                            }
                            onClick={handleConfirm}
                            loading={submitting}
                        >
                            {pendingAction === 'approve'
                                ? 'Xác nhận phê duyệt'
                                : 'Xác nhận từ chối'}
                        </Button>


                        <Button
                            component={Link}
                            to="/price-plans/execution"
                            variant="light"
                        >
                            Thực thi kế hoạch giá (US-15)
                        </Button>


                    </Group>
                </Stack>
            </Modal>
        </Stack>
    );
}
