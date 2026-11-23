
import { useState } from 'react';
import type { FormEvent } from 'react';
import axios from 'axios';
import DiscountNavigation from '../components/DiscountNavigation';



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
    Select,
    SimpleGrid,
    Stack,
    Text,
    Textarea,
    TextInput,
    Title,
} from '@mantine/core';

import { createDiscount } from '../api/discountApi';

import type {
    CreateDiscountRequest,
    DiscountResponse,
    DiscountType,
} from '../types/Discount';


interface DiscountForm {
    code: string;
    description: string;
    type: DiscountType;
    value: number | string;
    minAmount: number | string;
    validFrom: string;
    validTo: string;
    usageLimit: number | string;
}

type FieldErrors = Partial<
    Record<keyof DiscountForm, string>
>;

const initialForm: DiscountForm = {
    code: '',
    description: '',
    type: 'PERCENT',
    value: '',
    minAmount: 0,
    validFrom: '',
    validTo: '',
    usageLimit: '',
};

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
});

function validateForm(form: DiscountForm): FieldErrors {
    const errors: FieldErrors = {};

    // Mã: 3–30 ký tự chữ, số hoặc dấu _
    if (!/^[A-Za-z0-9_]{3,30}$/.test(form.code)) {
        errors.code =
            'Mã phải có 3–30 ký tự, chỉ gồm chữ Latin, số hoặc _.';
    }

    if (form.description.length > 255) {
        errors.description = 'Mô tả tối đa 255 ký tự.';
    }

    const value = Number(form.value);
    const minAmount = Number(form.minAmount);

    const moneyPattern = /^\d+(\.\d{1,2})?$/;

    if (
        form.value === '' ||
        !Number.isFinite(value) ||
        value <= 0 ||
        value > 9999999999.99 ||
        !moneyPattern.test(String(form.value))
    ) {
        errors.value =
            'Giá trị phải lớn hơn 0, tối đa 2 chữ số thập phân.';
    } else if (form.type === 'PERCENT' && value > 100) {
        errors.value = 'Phần trăm giảm không được vượt quá 100.';
    }

    if (
        !Number.isFinite(minAmount) ||
        minAmount < 0 ||
        minAmount > 9999999999.99 ||
        !moneyPattern.test(String(form.minAmount || 0))
    ) {
        errors.minAmount = 'Đơn tối thiểu không hợp lệ.';
    }

    if (!form.validFrom) {
        errors.validFrom = 'Vui lòng chọn ngày bắt đầu.';
    }

    if (!form.validTo) {
        errors.validTo = 'Vui lòng chọn ngày kết thúc.';
    }

    if (
        form.validFrom &&
        form.validTo &&
        form.validTo < form.validFrom
    ) {
        errors.validTo =
            'Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.';
    }

    if (form.usageLimit !== '') {
        const limit = Number(form.usageLimit);

        if (
            !Number.isInteger(limit) ||
            limit <= 0 ||
            limit > 2147483647
        ) {
            errors.usageLimit =
                'Giới hạn lượt dùng phải là số nguyên dương.';
        }
    }

    return errors;
}

function getApiError(error: unknown): string {
    if (axios.isAxiosError(error)) {
        if (!error.response) {
            return 'Không kết nối được Spring Boot.';
        }

        const body = error.response.data as
            | { message?: string }
            | undefined;

        switch (error.response.status) {
            case 400:
                return body?.message ?? 'Dữ liệu không hợp lệ.';

            case 403:
                return 'Bạn không có quyền tạo mã chiết khấu.';

            case 404:
            case 405:
                return 'API tạo mã chưa khả dụng. Kiểm tra profile local-demo.';

            case 409:
                return body?.message ?? 'Mã chiết khấu đã tồn tại.';

            default:
                return `Backend trả về HTTP ${error.response.status}.`;
        }
    }

    return error instanceof Error
        ? error.message
        : 'Không thể tạo mã chiết khấu.';
}

export default function CreateDiscountPage() {
    const [form, setForm] = useState<DiscountForm>({
        ...initialForm,
    });

    const [errors, setErrors] = useState<FieldErrors>({});
    const [apiError, setApiError] = useState<string | null>(null);

    const [created, setCreated] =
        useState<DiscountResponse | null>(null);

    const [modalOpened, setModalOpened] = useState(false);
    const [confirmed, setConfirmed] = useState(false);
    const [saving, setSaving] = useState(false);

    const [pending, setPending] =
        useState<CreateDiscountRequest | null>(null);

    // Kiểm tra form trước khi mở hộp thoại
    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (saving || created) return;

        const result = validateForm(form);
        setErrors(result);
        setApiError(null);

        if (Object.keys(result).length > 0) {
            return;
        }

        const request: CreateDiscountRequest = {
            code: form.code,
            description: form.description.trim() || null,
            type: form.type,
            value: Number(form.value),
            minAmount: Number(form.minAmount || 0),
            validFrom: form.validFrom,
            validTo: form.validTo,
            usageLimit:
                form.usageLimit === ''
                    ? null
                    : Number(form.usageLimit),
        };

        setPending(request);
        setConfirmed(false);
        setModalOpened(true);
    }

    function closeModal() {
        if (saving) return;

        setModalOpened(false);
        setConfirmed(false);
        setPending(null);
    }

    // Gửi POST sau khi người dùng xác nhận
    async function handleCreate() {
        if (!pending || !confirmed || saving) return;

        setSaving(true);
        setApiError(null);

        try {
            const result = await createDiscount(pending);

            setCreated(result);
            setModalOpened(false);
            setConfirmed(false);
            setPending(null);
        } catch (error: unknown) {
            setApiError(getApiError(error));
            setModalOpened(false);
            setConfirmed(false);
            setPending(null);
        } finally {
            setSaving(false);
        }
    }

    function resetForm() {
        setForm({ ...initialForm });
        setErrors({});
        setApiError(null);
        setCreated(null);
    }

    return (
        <Stack gap="lg">
            {/* Tiêu đề */}
            <Box>

                <Title order={2}>
                    Tạo mã chiết khấu
                </Title>

                <Text size="sm" c="dimmed" mt={4}>
                    Thiết lập chương trình giảm giá và điều kiện
                    áp dụng cho Home Rent.
                </Text>
                
                
            </Box>
            <DiscountNavigation />


            {/* Thông báo lỗi */}
            {apiError && (
                <Alert color="red" title="Không thể tạo mã">
                    {apiError}
                </Alert>
            )}

            {/* Kết quả sau khi tạo */}
            {created && (
                <Paper withBorder radius="sm" p="lg">
                    <Stack gap="md">
                        <Group justify="space-between">
                            <Box>
                                <Text fw={600} c="green">
                                    Tạo mã chiết khấu thành công
                                </Text>

                                <Title order={3} mt={4}>
                                    {created.code}
                                </Title>
                            </Box>

                            <Badge color="green" variant="light">
                                {created.status}
                            </Badge>
                        </Group>

                        <Divider />

                        <SimpleGrid cols={{ base: 1, sm: 2 }}>
                            <Box>
                                <Text size="xs" c="dimmed">
                                    Mã bản ghi
                                </Text>
                                <Text fw={600}>#{created.discountId}</Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Loại chiết khấu
                                </Text>
                                <Text fw={600}>
                                    {created.type === 'PERCENT'
                                        ? 'Theo phần trăm'
                                        : 'Số tiền cố định'}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Giá trị giảm
                                </Text>
                                <Text fw={600}>
                                    {created.type === 'PERCENT'
                                        ? `${created.value}%`
                                        : moneyFormatter.format(Number(created.value))}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Người tạo
                                </Text>
                                <Text fw={600}>
                                    User ID: {created.createdBy}
                                </Text>
                            </Box>
                        </SimpleGrid>

                        <Group>
                            <Button variant="light" onClick={resetForm}>
                                Tạo mã khác
                            </Button>
                        </Group>
                    </Stack>
                </Paper>
            )}

            {/* Form tạo mã */}
            <Paper withBorder radius="sm" p="lg">
                <form onSubmit={handleSubmit}>
                    <Stack gap="lg">
                        <Box>
                            <Text fw={600} mb="sm">
                                Thông tin mã chiết khấu
                            </Text>

                            <SimpleGrid cols={{ base: 1, sm: 2 }}>
                                <TextInput
                                    label="Mã chiết khấu"
                                    placeholder="VD: WINTER15"
                                    description="3–30 ký tự, chữ Latin, số hoặc _"
                                    required
                                    value={form.code}
                                    onChange={(event) => {
                                        const value = event.currentTarget.value.toUpperCase();

                                        setForm((prev) => ({
                                            ...prev,
                                            code: value,
                                        }));
                                    }}
                                    error={errors.code}
                                    disabled={saving || !!created}
                                    maxLength={30}
                                />

                                <Select
                                    label="Loại chiết khấu"
                                    description="Chọn phần trăm hoặc số tiền cố định"
                                    data={[
                                        {
                                            value: 'PERCENT',
                                            label: 'Theo phần trăm (%)',
                                        },
                                        {
                                            value: 'FIXED',
                                            label: 'Số tiền cố định (VND)',
                                        },
                                    ]}
                                    value={form.type}
                                    onChange={(value) => {
                                        if (value === 'PERCENT' || value === 'FIXED') {
                                            setForm((prev) => ({
                                                ...prev,
                                                type: value,
                                                value: '',
                                            }));
                                        }
                                    }}
                                    allowDeselect={false}
                                    disabled={saving || !!created}
                                    required
                                />
                            </SimpleGrid>

                            <Textarea
                                label="Mô tả"
                                placeholder="Mô tả chương trình ưu đãi..."
                                value={form.description}
                                onChange={(event) => {
                                    const value = event.currentTarget.value;

                                    setForm((prev) => ({
                                        ...prev,
                                        description: value,
                                    }));
                                }}
                                error={errors.description}
                                disabled={saving || !!created}
                                maxLength={255}
                                autosize
                                minRows={2}
                                mt="md"
                            />
                        </Box>

                        <Divider />

                        <Box>
                            <Text fw={600} mb="sm">
                                Giá trị và điều kiện giảm
                            </Text>

                            <SimpleGrid cols={{ base: 1, sm: 2 }}>
                                <NumberInput
                                    label={
                                        form.type === 'PERCENT'
                                            ? 'Giá trị giảm (%)'
                                            : 'Giá trị giảm (VND)'
                                    }
                                    description="Nhập giá trị giảm theo loại đã chọn"
                                    placeholder={
                                        form.type === 'PERCENT'
                                            ? 'VD: 15'
                                            : 'VD: 100000'
                                    }
                                    value={form.value}
                                    onChange={(value) =>
                                        setForm((prev) => ({
                                            ...prev,
                                            value,
                                        }))
                                    }
                                    min={0}
                                    max={
                                        form.type === 'PERCENT'
                                            ? 100
                                            : 9999999999.99
                                    }
                                    decimalScale={2}
                                    allowNegative={false}
                                    error={errors.value}
                                    disabled={saving || !!created}
                                    required
                                />

                                <NumberInput
                                    label="Giá trị đơn tối thiểu (VND)"
                                    description="Nhập 0 nếu không yêu cầu đơn tối thiểu"
                                    value={form.minAmount}
                                    onChange={(value) =>
                                        setForm((prev) => ({
                                            ...prev,
                                            minAmount: value,
                                        }))
                                    }
                                    min={0}
                                    max={9999999999.99}
                                    decimalScale={2}
                                    allowNegative={false}
                                    error={errors.minAmount}
                                    disabled={saving || !!created}
                                />
                            </SimpleGrid>
                        </Box>

                        <Divider />

                        <Box>
                            <Text fw={600} mb="sm">
                                Thời gian và giới hạn
                            </Text>

                            <SimpleGrid cols={{ base: 1, sm: 2 }}>
                                <TextInput
                                    label="Ngày bắt đầu"
                                    type="date"
                                    value={form.validFrom}

                                    onChange={(event) => {
                                        const value = event.currentTarget.value;

                                        setForm((prev) => ({
                                            ...prev,
                                            validFrom: value,
                                        }));
                                    }}

                                    error={errors.validFrom}
                                    disabled={saving || !!created}
                                    required
                                />

                                <TextInput
                                    label="Ngày kết thúc"
                                    type="date"
                                    value={form.validTo}

                                    onChange={(event) => {
                                        const value = event.currentTarget.value;

                                        setForm((prev) => ({
                                            ...prev,
                                            validTo: value,
                                        }));
                                    }}

                                    error={errors.validTo}
                                    disabled={saving || !!created}
                                    required
                                />
                            </SimpleGrid>

                            <NumberInput
                                label="Giới hạn lượt sử dụng"
                                description="Để trống nếu không giới hạn"
                                placeholder="Không giới hạn"
                                value={form.usageLimit}
                                onChange={(value) =>
                                    setForm((prev) => ({
                                        ...prev,
                                        usageLimit: value,
                                    }))
                                }
                                min={1}
                                max={2147483647}
                                allowDecimal={false}
                                allowNegative={false}
                                error={errors.usageLimit}
                                disabled={saving || !!created}
                                mt="md"
                                w={{ base: '100%', sm: 260 }}
                            />
                        </Box>

                        <Divider />

                        <Group justify="flex-end">
                            <Button
                                type="button"
                                variant="default"
                                onClick={resetForm}
                                disabled={saving}
                            >
                                {created ? 'Nhập mã mới' : 'Làm mới'}
                            </Button>

                            <Button
                                type="submit"
                                loading={saving}
                                disabled={!!created}
                            >
                                Tạo mã chiết khấu
                            </Button>
                        </Group>
                    </Stack>
                </form>
            </Paper>

            {/* Hộp thoại xác nhận ghi database */}
            <Modal
                opened={modalOpened}
                onClose={closeModal}
                title="Xác nhận tạo mã chiết khấu"
                centered
                closeOnClickOutside={!saving}
                closeOnEscape={!saving}
                withCloseButton={!saving}
            >
                <Stack gap="md">
                    <Text size="sm">
                        Vui lòng kiểm tra thông tin trước khi tạo
                        mã giảm giá trong hệ thống.
                    </Text>

                    {pending && (
                        <Paper withBorder radius="sm" p="md">
                            <Stack gap="xs">
                                <Group justify="space-between">
                                    <Text size="sm" c="dimmed">
                                        Mã
                                    </Text>
                                    <Text size="sm" fw={600}>
                                        {pending.code}
                                    </Text>
                                </Group>

                                <Group justify="space-between">
                                    <Text size="sm" c="dimmed">
                                        Giá trị giảm
                                    </Text>
                                    <Text size="sm" fw={600}>
                                        {pending.type === 'PERCENT'
                                            ? `${pending.value}%`
                                            : moneyFormatter.format(pending.value)}
                                    </Text>
                                </Group>

                                <Group justify="space-between">
                                    <Text size="sm" c="dimmed">
                                        Hiệu lực
                                    </Text>
                                    <Text size="sm">
                                        {pending.validFrom} đến {pending.validTo}
                                    </Text>
                                </Group>
                            </Stack>
                        </Paper>
                    )}

                    <Alert color="yellow" title="Lưu ý">
                        Xác nhận sẽ gửi POST và tạo một bản ghi
                        mới trong bảng discounts của SQL Server.
                    </Alert>

                    <Checkbox
                        checked={confirmed}
                        onChange={(event) =>
                            setConfirmed(event.currentTarget.checked)
                        }
                        disabled={saving}
                        label="Tôi đã kiểm tra thông tin và đồng ý tạo mã."
                    />

                    <Group justify="flex-end">
                        <Button
                            variant="default"
                            onClick={closeModal}
                            disabled={saving}
                        >
                            Hủy
                        </Button>

                        <Button
                            onClick={handleCreate}
                            disabled={!confirmed}
                            loading={saving}
                        >
                            Xác nhận tạo
                        </Button>
                    </Group>
                </Stack>
            </Modal>
        </Stack>
    );
}
