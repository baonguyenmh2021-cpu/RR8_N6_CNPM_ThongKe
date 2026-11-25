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
    SimpleGrid,
    Stack,
    Text,
    Textarea,
    TextInput,
    Title,
} from '@mantine/core';

import { createMarketingStrategy } from '../api/marketingStrategyApi';

import type {
    CreateMarketingStrategyRequest,
    MarketingStrategyResponse,
} from '../types/MarketingStrategy';

import { Link } from 'react-router-dom';

const MAX_BUDGET = 999999999999.99;

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 2,
});

function formatMoney(value: number | string): string {
    const number = Number(value);

    return Number.isFinite(number)
        ? moneyFormatter.format(number)
        : 'Không hợp lệ';
}

function formatDateTime(value: string): string {
    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleString('vi-VN');
}

function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const data = error.response?.data as
            | { message?: string }
            | undefined;

        if (data?.message) {
            return data.message;
        }

        if (!error.response) {
            return 'Không kết nối được Backend Spring Boot.';
        }

        switch (error.response.status) {
            case 400:
                return 'Dữ liệu chiến lược không hợp lệ.';
            case 403:
                return 'Tài khoản demo không có quyền Manager.';
            case 404:
                return 'Không tìm thấy Discount ID đã nhập.';
            case 409:
                return 'Discount không đủ điều kiện liên kết.';
            default:
                return `Backend trả về HTTP ${error.response.status}.`;
        }
    }

    return error instanceof Error
        ? error.message
        : 'Không thể tạo chiến lược Marketing.';
}

function hasValidDecimal(
    value: string | number,
    max: number
): boolean {
    if (value === '') return false;

    const number = Number(value);
    const decimalPart = String(value).split('.')[1] ?? '';

    return (
        Number.isFinite(number) &&
        number >= 0 &&
        number <= max &&
        decimalPart.length <= 2
    );
}

export default function CreateMarketingStrategyPage() {
    const [title, setTitle] = useState('');
    const [targetSegment, setTargetSegment] = useState('');
    const [description, setDescription] = useState('');

    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');

    const [budget, setBudget] =
        useState<string | number>(0);

    const [discountId, setDiscountId] =
        useState<string | number>('');

    const [expectedBookings, setExpectedBookings] =
        useState<string | number>('');

    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [createdStrategy, setCreatedStrategy] =
        useState<MarketingStrategyResponse | null>(null);

    async function handleCreate() {
        if (submitting) return;

        setError(null);
        setCreatedStrategy(null);

        // 1. Kiểm tra tên chiến lược
        const cleanTitle = title.trim();

        if (!cleanTitle || cleanTitle.length > 150) {
            setError(
                'Tên chiến lược bắt buộc và tối đa 150 ký tự.'
            );
            return;
        }

        // 2. Kiểm tra đối tượng mục tiêu
        if (targetSegment.trim().length > 150) {
            setError(
                'Đối tượng mục tiêu tối đa 150 ký tự.'
            );
            return;
        }

        // 3. Kiểm tra thời gian
        if (!startDate || !endDate) {
            setError(
                'Vui lòng nhập ngày bắt đầu và ngày kết thúc.'
            );
            return;
        }

        if (endDate < startDate) {
            setError(
                'Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.'
            );
            return;
        }

        // 4. Kiểm tra ngân sách DECIMAL(14,2)
        if (!hasValidDecimal(budget, MAX_BUDGET)) {
            setError(
                'Ngân sách phải từ 0 đến 999.999.999.999,99 VND và tối đa 2 chữ số thập phân.'
            );
            return;
        }

        // 5. Discount ID tùy chọn
        let parsedDiscountId: number | null = null;

        if (discountId !== '') {
            const value = Number(discountId);

            if (
                !Number.isSafeInteger(value) ||
                value <= 0
            ) {
                setError('Discount ID phải là số nguyên dương.');
                return;
            }

            parsedDiscountId = value;
        }

        // 6. Booking kỳ vọng tùy chọn
        let parsedExpectedBookings: number | null = null;

        if (expectedBookings !== '') {
            const value = Number(expectedBookings);

            if (
                !Number.isSafeInteger(value) ||
                value < 0 ||
                value > 2147483647
            ) {
                setError(
                    'Booking kỳ vọng phải là số nguyên không âm.'
                );
                return;
            }

            parsedExpectedBookings = value;
        }

        // 7. Tạo JSON đúng với Backend DTO
        const request: CreateMarketingStrategyRequest = {
            title: cleanTitle,
            targetSegment: targetSegment.trim() || null,
            description: description.trim() || null,
            startDate,
            endDate,
            budget: Number(budget),
            discountId: parsedDiscountId,
            expectedBookings: parsedExpectedBookings,
        };

        // 8. Gọi API
        setSubmitting(true);

        try {
            const result =
                await createMarketingStrategy(request);

            setCreatedStrategy(result);
        } catch (err: unknown) {
            setError(getErrorMessage(err));
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <Stack gap="lg">
            {/* Header: Tiêu đề và điều hướng */}
            <Stack gap="md">
                <Group justify="space-between" align="start">
                    <Box>
                        <Title order={2}>
                            Tạo chiến lược Marketing
                        </Title>

                    </Box>

                    <Badge color="blue" variant="light">
                        MANAGER
                    </Badge>
                </Group>

                {/* Các nút điều hướng Marketing */}
                <Group gap="xs" wrap="wrap">
                    <Button variant="light">
                        Tạo chiến lược
                    </Button>

                    <Button
                        component={Link}
                        to="/marketing/list"
                        variant="subtle"
                    >
                        Danh sách chiến lược
                    </Button>

                    <Button
                        component={Link}
                        to="/marketing/effectiveness"
                        variant="subtle"
                    >
                        Hiệu quả Marketing
                    </Button>
                </Group>
            </Stack>

            {/* Form */}
            <Paper withBorder radius="sm" p="lg">
                <Stack gap="lg">
                    <Box>
                        <Title order={4}>
                            Thông tin chiến lược
                        </Title>

                        <Text size="xs" c="dimmed" mt={5}>
                            Các trường có dấu * là bắt buộc.
                        </Text>
                    </Box>

                    <TextInput
                        label="Tên chiến lược"
                        placeholder="Ví dụ: Ưu đãi cuối năm"
                        value={title}
                        onChange={(event) =>
                            setTitle(event.currentTarget.value)
                        }
                        maxLength={150}
                        required
                    />

                    <TextInput
                        label="Đối tượng mục tiêu"
                        placeholder="Ví dụ: Sinh viên, khách thuê dài hạn"
                        description="Có thể để trống, tối đa 150 ký tự."
                        value={targetSegment}
                        onChange={(event) =>
                            setTargetSegment(
                                event.currentTarget.value
                            )
                        }
                        maxLength={150}
                    />

                    <Textarea
                        label="Mô tả chiến lược"
                        placeholder="Mô tả mục tiêu và hoạt động Marketing..."
                        minRows={3}
                        maxRows={6}
                        value={description}
                        onChange={(event) =>
                            setDescription(
                                event.currentTarget.value
                            )
                        }
                    />

                    <Divider
                        label="Thời gian triển khai"
                        labelPosition="left"
                    />

                    <SimpleGrid cols={{ base: 1, sm: 2 }}>
                        <TextInput
                            type="date"
                            label="Ngày bắt đầu"
                            value={startDate}
                            onChange={(event) =>
                                setStartDate(
                                    event.currentTarget.value
                                )
                            }
                            required
                        />

                        <TextInput
                            type="date"
                            label="Ngày kết thúc"
                            min={startDate || undefined}
                            value={endDate}
                            onChange={(event) =>
                                setEndDate(
                                    event.currentTarget.value
                                )
                            }
                            required
                        />
                    </SimpleGrid>

                    <Divider
                        label="Ngân sách và mục tiêu"
                        labelPosition="left"
                    />

                    <SimpleGrid cols={{ base: 1, sm: 2 }}>
                        <NumberInput
                            label="Ngân sách dự kiến (VND)"
                            placeholder="Ví dụ: 5000000"
                            description="Ngân sách không được âm."
                            value={budget}
                            onChange={setBudget}
                            min={0}
                            max={MAX_BUDGET}
                            decimalScale={2}
                            allowNegative={false}
                            required
                        />

                        <NumberInput
                            label="Booking kỳ vọng"
                            placeholder="Ví dụ: 40"
                            description="Có thể để trống."
                            value={expectedBookings}
                            onChange={setExpectedBookings}
                            min={0}
                            max={2147483647}
                            allowNegative={false}
                            allowDecimal={false}
                        />
                    </SimpleGrid>

                    <Divider
                        label="Liên kết mã chiết khấu"
                        labelPosition="left"
                    />

                    <NumberInput
                        label="Discount ID"
                        placeholder="Nhập ID mã giảm giá (nếu có)"
                        description="Để trống nếu không liên kết Discount. Nếu có, mã phải Active và bao phủ toàn bộ thời gian chiến lược."
                        value={discountId}
                        onChange={setDiscountId}
                        min={1}
                        allowNegative={false}
                        allowDecimal={false}
                    />

                    <Alert color="gray" title="Chi phí thực tế">
                        Khi tạo chiến lược, Backend tự lưu
                        actualSpend = 0. Không cần nhập
                        chi phí thực tế tại bước này.
                    </Alert>

                    {error && (
                        <Alert
                            color="red"
                            title="Không thể tạo chiến lược"
                        >
                            {error}
                        </Alert>
                    )}

                    <Group justify="flex-end">
                        <Button
                            onClick={handleCreate}
                            loading={submitting}
                        >
                            Tạo chiến lược
                        </Button>
                    </Group>
                </Stack>
            </Paper>

            {/* Kết quả sau khi tạo thành công */}
            {createdStrategy && (
                <Paper withBorder radius="sm" p="lg">
                    <Stack gap="md">
                        <Group justify="space-between">
                            <Box>
                                <Title order={4}>
                                    Tạo chiến lược thành công
                                </Title>

                                <Text size="xs" c="dimmed" mt={4}>
                                    Mã chiến lược: #
                                    {createdStrategy.strategyId}
                                </Text>
                            </Box>

                            <Badge
                                color="yellow"
                                variant="light"
                            >
                                {createdStrategy.status}
                            </Badge>
                        </Group>

                        <Alert color="green">
                            Backend đã tạo chiến lược
                            Marketing và trả về kết quả.
                        </Alert>

                        <Divider />

                        <Text fw={600}>
                            {createdStrategy.title}
                        </Text>

                        <SimpleGrid cols={{ base: 1, sm: 2 }}>
                            <Box>
                                <Text size="xs" c="dimmed">
                                    Đối tượng mục tiêu
                                </Text>

                                <Text size="sm">
                                    {createdStrategy.targetSegment ||
                                        'Không xác định'}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Trạng thái
                                </Text>

                                <Text fw={600} size="sm">
                                    {createdStrategy.status}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Ngày bắt đầu
                                </Text>

                                <Text size="sm">
                                    {createdStrategy.startDate}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Ngày kết thúc
                                </Text>

                                <Text size="sm">
                                    {createdStrategy.endDate}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Ngân sách dự kiến
                                </Text>

                                <Text fw={600} size="sm">
                                    {formatMoney(
                                        createdStrategy.budget
                                    )}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Chi phí thực tế
                                </Text>

                                <Text size="sm">
                                    {formatMoney(
                                        createdStrategy.actualSpend
                                    )}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Discount liên kết
                                </Text>

                                <Text size="sm">
                                    {createdStrategy.discountId === null
                                        ? 'Không liên kết'
                                        : `ID ${createdStrategy.discountId}`}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Booking kỳ vọng
                                </Text>

                                <Text size="sm">
                                    {createdStrategy.expectedBookings ??
                                        'Chưa thiết lập'}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Người tạo
                                </Text>

                                <Text size="sm">
                                    User ID {createdStrategy.createdBy}
                                </Text>
                            </Box>

                            <Box>
                                <Text size="xs" c="dimmed">
                                    Thời gian tạo
                                </Text>

                                <Text size="sm">
                                    {formatDateTime(
                                        createdStrategy.createdAt
                                    )}
                                </Text>
                            </Box>
                        </SimpleGrid>

                        {createdStrategy.description && (
                            <Box>
                                <Text size="xs" c="dimmed">
                                    Mô tả
                                </Text>

                                <Text size="sm">
                                    {createdStrategy.description}
                                </Text>
                            </Box>
                        )}
                    </Stack>
                </Paper>
            )}
        </Stack>
    );
}