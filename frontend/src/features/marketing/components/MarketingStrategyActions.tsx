
import { useState } from 'react';
import axios from 'axios';

import {
    Alert,
    Button,
    Checkbox,
    Group,
    Loader,
    Menu,
    Modal,
    NumberInput,
    SimpleGrid,
    Stack,
    Text,
    Textarea,
    TextInput,
} from '@mantine/core';

import { getMarketingEffectiveness } from '../api/marketingEffectivenessApi';

import {
    updateMarketingStrategy,
    changeMarketingStrategyStatus,
} from '../api/marketingStrategyManagementApi';

import type {
    MarketingStrategyStatus,
    MarketingStrategySummaryResponse,
} from '../types/MarketingStrategy';

import type {
    UpdateMarketingStrategyRequest,
} from '../types/MarketingStrategyManagement';

type EditForm = {
    title: string;
    targetSegment: string;
    description: string;
    startDate: string;
    endDate: string;
    budget: number | string;
    actualSpend: number | string;
    discountId: number | string;
    expectedBookings: number | string;
};

type Props = {
    item: MarketingStrategySummaryResponse;
    onUpdated: (message: string) => void;
};

const MAX_MONEY = 999999999999.99;

function errorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const data = error.response?.data as
            { message?: string } | undefined;

        if (data?.message) return data.message;

        if (!error.response) {
            return 'Không kết nối được Backend.';
        }

        return `Backend trả về HTTP ${error.response.status}.`;
    }

    return error instanceof Error
        ? error.message
        : 'Thao tác không thành công.';
}

function validMoney(value: number | string): boolean {
    if (value === '') return false;

    const number = Number(value);
    const text = String(value);

    return (
        Number.isFinite(number) &&
        number >= 0 &&
        number <= MAX_MONEY &&
        /^\d+(\.\d{1,2})?$/.test(text)
    );
}

function validOptionalInteger(
    value: number | string,
    minimum: number
): boolean {
    return (
        value === '' ||
        (
            Number.isSafeInteger(Number(value)) &&
            Number(value) >= minimum &&
            Number(value) <= 2147483647
        )
    );
}

export default function MarketingStrategyActions({
    item,
    onUpdated,
}: Props) {
    const [dialog, setDialog] =
        useState<'edit' | 'status' | null>(null);

    const [nextStatus, setNextStatus] =
        useState<MarketingStrategyStatus | null>(null);

    const [form, setForm] = useState<EditForm | null>(null);
    const [loadingDetails, setLoadingDetails] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [confirmedDescription, setConfirmedDescription] =
        useState(false);

    const canModify =
        item.status === 'Planned' ||
        item.status === 'Running';

    function closeDialog() {
        if (saving) return;

        setDialog(null);
        setError(null);
    }

    function openStatus(status: MarketingStrategyStatus) {
        if (!canModify) return;

        setNextStatus(status);
        setError(null);
        setDialog('status');
    }

    async function openEdit() {
        if (!canModify) return;

        setDialog('edit');
        setError(null);
        setForm(null);
        setConfirmedDescription(false);
        setLoadingDetails(true);

        try {
            // Tận dụng API US-12 để đọc các trường bổ sung
            const detail = await getMarketingEffectiveness(
                item.strategyId
            );

            setForm({
                title: item.title,
                targetSegment: item.targetSegment ?? '',
                description: '',
                startDate: item.startDate,
                endDate: item.endDate,
                budget: item.budget,
                actualSpend: detail.actualSpend,
                discountId: detail.discountId ?? '',
                expectedBookings: detail.expectedBookings ?? '',
            });
        } catch (err: unknown) {
            setError(errorMessage(err));
        } finally {
            setLoadingDetails(false);
        }
    }

    function changeField<K extends keyof EditForm>(
        field: K,
        value: EditForm[K]
    ) {
        setForm((old) =>
            old ? { ...old, [field]: value } : old
        );
    }

    async function confirmStatus() {
        if (!nextStatus || saving) return;

        setSaving(true);
        setError(null);

        try {
            const result = await changeMarketingStrategyStatus(
                item.strategyId,
                { status: nextStatus }
            );

            setDialog(null);
            onUpdated(
                `Chiến lược #${result.strategyId} đã chuyển sang ${result.status}.`
            );
        } catch (err: unknown) {
            setError(errorMessage(err));
        } finally {
            setSaving(false);
        }
    }

    async function saveDetails() {
        if (!form || saving) return;

        const title = form.title.trim();

        if (!title || title.length > 150) {
            setError('Tên chiến lược phải có 1–150 ký tự.');
            return;
        }

        if (form.targetSegment.trim().length > 150) {
            setError('Đối tượng mục tiêu tối đa 150 ký tự.');
            return;
        }

        if (
            !form.startDate ||
            !form.endDate ||
            form.endDate < form.startDate
        ) {
            setError('Khoảng thời gian không hợp lệ.');
            return;
        }

        if (
            !validMoney(form.budget) ||
            !validMoney(form.actualSpend)
        ) {
            setError(
                'Ngân sách và chi phí phải từ 0, tối đa 2 chữ số thập phân.'
            );
            return;
        }

        if (!validOptionalInteger(form.discountId, 1)) {
            setError('Discount ID phải là số nguyên dương.');
            return;
        }

        if (!validOptionalInteger(form.expectedBookings, 0)) {
            setError('Booking kỳ vọng phải là số nguyên không âm.');
            return;
        }

        if (!confirmedDescription) {
            setError(
                'Vui lòng xác nhận nội dung mô tả trước khi lưu.'
            );
            return;
        }

        const request: UpdateMarketingStrategyRequest = {
            title,
            targetSegment:
                form.targetSegment.trim() || null,
            description:
                form.description.trim() || null,
            startDate: form.startDate,
            endDate: form.endDate,
            budget: Number(form.budget),
            actualSpend: Number(form.actualSpend),
            discountId:
                form.discountId === ''
                    ? null
                    : Number(form.discountId),
            expectedBookings:
                form.expectedBookings === ''
                    ? null
                    : Number(form.expectedBookings),
        };

        setSaving(true);
        setError(null);

        try {
            const result = await updateMarketingStrategy(
                item.strategyId,
                request
            );

            setDialog(null);
            onUpdated(
                `Đã cập nhật chiến lược #${result.strategyId}: ${result.title}.`
            );
        } catch (err: unknown) {
            setError(errorMessage(err));
        } finally {
            setSaving(false);
        }
    }

    return (
        <>
            <Menu position="bottom-end" withinPortal>
                <Menu.Target>
                    <Button
                        size="xs"
                        variant="outline"
                    >
                        Quản lý
                    </Button>
                </Menu.Target>

                <Menu.Dropdown>
                    <Menu.Label>
                        Chiến lược #{item.strategyId}
                    </Menu.Label>

                    <Menu.Item
                        disabled={!canModify}
                        onClick={openEdit}
                    >
                        Chỉnh sửa thông tin
                    </Menu.Item>

                    {item.status === 'Planned' && (
                        <Menu.Item
                            onClick={() => openStatus('Running')}
                        >
                            Bắt đầu triển khai
                        </Menu.Item>
                    )}

                    {item.status === 'Running' && (
                        <Menu.Item
                            onClick={() => openStatus('Finished')}
                        >
                            Hoàn thành chiến lược
                        </Menu.Item>
                    )}

                    {canModify && (
                        <Menu.Item
                            color="red"
                            onClick={() => openStatus('Cancelled')}
                        >
                            Hủy chiến lược
                        </Menu.Item>
                    )}

                    {!canModify && (
                        <Menu.Item disabled>
                            Trạng thái cuối — không thể sửa
                        </Menu.Item>
                    )}
                </Menu.Dropdown>
            </Menu>

            {/* Xác nhận chuyển trạng thái */}
            <Modal
                opened={dialog === 'status'}
                onClose={closeDialog}
                title="Xác nhận chuyển trạng thái"
                centered
                closeOnClickOutside={!saving}
                withCloseButton={!saving}
            >
                <Stack gap="md">
                    <Text size="sm">
                        Chiến lược <strong>#{item.strategyId}</strong>
                        {' '}đang ở trạng thái{' '}
                        <strong>{item.status}</strong>.
                    </Text>

                    <Text size="sm">
                        Trạng thái mới:{' '}
                        <strong>{nextStatus}</strong>
                    </Text>

                    {nextStatus === 'Cancelled' && (
                        <Alert color="red">
                            Hủy chiến lược là thao tác kết thúc
                            vòng đời. Sau khi hủy, chiến lược
                            không thể tiếp tục chỉnh sửa.
                        </Alert>
                    )}

                    {error && (
                        <Alert color="red">{error}</Alert>
                    )}

                    <Group justify="flex-end">
                        <Button
                            variant="default"
                            onClick={closeDialog}
                            disabled={saving}
                        >
                            Quay lại
                        </Button>

                        <Button
                            color={
                                nextStatus === 'Cancelled'
                                    ? 'red'
                                    : 'blue'
                            }
                            loading={saving}
                            onClick={confirmStatus}
                        >
                            Xác nhận
                        </Button>
                    </Group>
                </Stack>
            </Modal>

            {/* Chỉnh sửa nội dung chiến lược */}
            <Modal
                opened={dialog === 'edit'}
                onClose={closeDialog}
                title={`Chỉnh sửa chiến lược #${item.strategyId}`}
                centered
                size="lg"
                closeOnClickOutside={!saving}
                withCloseButton={!saving}
            >
                {loadingDetails && (
                    <Group justify="center" p="lg">
                        <Loader size="sm" />
                        <Text size="sm">
                            Đang lấy dữ liệu chiến lược...
                        </Text>
                    </Group>
                )}

                {!loadingDetails && form && (
                    <Stack gap="md">
                        <Alert color="yellow" title="Lưu ý khi chỉnh sửa">
                            Backend chưa có API đọc mô tả chi tiết
                            của một chiến lược. Trường mô tả
                            bên dưới không được tự động điền.
                            Hãy nhập lại mô tả hiện tại nếu
                            muốn giữ nguyên, hoặc để trống nếu
                            thực sự muốn xóa mô tả.
                        </Alert>

                        <TextInput
                            label="Tên chiến lược"
                            required
                            maxLength={150}
                            value={form.title}
                            onChange={(e) =>
                                changeField(
                                    'title',
                                    e.currentTarget.value
                                )
                            }
                        />

                        <TextInput
                            label="Đối tượng mục tiêu"
                            maxLength={150}
                            value={form.targetSegment}
                            onChange={(e) =>
                                changeField(
                                    'targetSegment',
                                    e.currentTarget.value
                                )
                            }
                        />

                        <Textarea
                            label="Mô tả sau cập nhật"
                            description="Nhập toàn bộ mô tả bạn muốn lưu."
                            minRows={3}
                            value={form.description}
                            onChange={(e) =>
                                changeField(
                                    'description',
                                    e.currentTarget.value
                                )
                            }
                        />

                        <SimpleGrid cols={{ base: 1, sm: 2 }}>
                            <TextInput
                                type="date"
                                label="Ngày bắt đầu"
                                required
                                value={form.startDate}
                                onChange={(e) =>
                                    changeField(
                                        'startDate',
                                        e.currentTarget.value
                                    )
                                }
                            />

                            <TextInput
                                type="date"
                                label="Ngày kết thúc"
                                required
                                min={form.startDate || undefined}
                                value={form.endDate}
                                onChange={(e) =>
                                    changeField(
                                        'endDate',
                                        e.currentTarget.value
                                    )
                                }
                            />

                            <NumberInput
                                label="Ngân sách (VND)"
                                required
                                min={0}
                                max={MAX_MONEY}
                                decimalScale={2}
                                allowNegative={false}
                                value={form.budget}
                                onChange={(value) =>
                                    changeField('budget', value)
                                }
                            />

                            <NumberInput
                                label="Chi phí thực tế (VND)"
                                required
                                min={0}
                                max={MAX_MONEY}
                                decimalScale={2}
                                allowNegative={false}
                                value={form.actualSpend}
                                onChange={(value) =>
                                    changeField('actualSpend', value)
                                }
                            />

                            <NumberInput
                                label="Discount ID (tùy chọn)"
                                min={1}
                                allowDecimal={false}
                                allowNegative={false}
                                value={form.discountId}
                                onChange={(value) =>
                                    changeField('discountId', value)
                                }
                            />

                            <NumberInput
                                label="Booking kỳ vọng"
                                min={0}
                                max={2147483647}
                                allowDecimal={false}
                                allowNegative={false}
                                value={form.expectedBookings}
                                onChange={(value) =>
                                    changeField('expectedBookings', value)
                                }
                            />
                        </SimpleGrid>

                        <Checkbox
                            checked={confirmedDescription}
                            onChange={(e) =>
                                setConfirmedDescription(
                                    e.currentTarget.checked
                                )
                            }
                            label="Tôi đã kiểm tra mô tả và đồng ý thay thế nội dung mô tả hiện tại bằng nội dung đã nhập."
                        />

                        {error && (
                            <Alert color="red" title="Không thể cập nhật">
                                {error}
                            </Alert>
                        )}

                        <Group justify="flex-end">
                            <Button
                                variant="default"
                                onClick={closeDialog}
                                disabled={saving}
                            >
                                Hủy
                            </Button>

                            <Button
                                loading={saving}
                                disabled={!confirmedDescription}
                                onClick={saveDetails}
                            >
                                Lưu thay đổi
                            </Button>
                        </Group>
                    </Stack>
                )}

                {!loadingDetails && !form && error && (
                    <Alert color="red">{error}</Alert>
                )}
            </Modal>
        </>
    );
}
