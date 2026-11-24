
import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';

import {
    Alert,
    Badge,
    Box,
    Button,
    Group,
    Loader,
    NumberInput,
    Paper,
    Select,
    Stack,
    Text,
    TextInput,
    Textarea,
    Title,
} from '@mantine/core';

import { getOccupancyCategories } from '../../occupancy/api/occupancyApi';

import type {
    OccupancyCategoryResponse,
} from '../../occupancy/types/Occupancy';

import { createPricePlan } from '../api/pricePlanApi';

import type {
    CreatePricePlanRequest,
    PricePlanResponse,
} from '../types/PricePlan';

// Định dạng ngày theo giờ địa phương
function formatDateInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

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
    maximumFractionDigits: 2,
});

function formatMoney(value: number | string | null): string {
    if (value === null) return 'Chưa có dữ liệu';

    const number = Number(value);

    return Number.isFinite(number)
        ? moneyFormatter.format(number)
        : 'Không hợp lệ';
}

// Kiểm tra giá các phòng cùng loại có đồng nhất không
function hasUniformPrice(
    category: OccupancyCategoryResponse
): boolean {
    if (
        category.minCurrentPrice === null ||
        category.maxCurrentPrice === null
    ) {
        return false;
    }

    const min = Number(category.minCurrentPrice);
    const max = Number(category.maxCurrentPrice);

    return (
        Number.isFinite(min) &&
        Number.isFinite(max) &&
        min > 0 &&
        min === max
    );
}

// Xử lý lỗi API
function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const responseData = error.response?.data as
            { message?: string } | undefined;

        if (responseData?.message) {
            return responseData.message;
        }

        if (!error.response) {
            return 'Không kết nối được Spring Boot. Hãy kiểm tra Backend và profile local-demo.';
        }

        return `Backend trả về HTTP ${error.response.status}.`;
    }

    return error instanceof Error
        ? error.message
        : 'Không thể tạo kế hoạch giá.';
}

function formatDateTime(value: string): string {
    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? value
        : date.toLocaleString('vi-VN');
}

export default function CreatePricePlanPage() {
    const [categories, setCategories] = useState<
        OccupancyCategoryResponse[]
    >([]);

    const [categoriesLoading, setCategoriesLoading] =
        useState(true);

    const [categoryError, setCategoryError] =
        useState<string | null>(null);

    const [categoryId, setCategoryId] =
        useState<string | null>(null);

    const [title, setTitle] = useState('');

    const [newPrice, setNewPrice] =
        useState<string | number>('');

    const [effectiveFrom, setEffectiveFrom] =
        useState('');

    const [effectiveTo, setEffectiveTo] =
        useState('');

    const [reason, setReason] = useState('');

    const [submitting, setSubmitting] = useState(false);

    const [formError, setFormError] =
        useState<string | null>(null);

    const [createdPlan, setCreatedPlan] =
        useState<PricePlanResponse | null>(null);

    // Tải danh sách loại phòng từ API US-08
    useEffect(() => {
        const controller = new AbortController();

        setCategoriesLoading(true);

        getOccupancyCategories(
            initialFrom,
            initialTo,
            controller.signal
        )
            .then((result) => {
                if (!controller.signal.aborted) {
                    setCategories(result);
                }
            })
            .catch((error: unknown) => {
                if (!controller.signal.aborted) {
                    setCategoryError(getErrorMessage(error));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setCategoriesLoading(false);
                }
            });

        return () => controller.abort();
    }, []);

    // Tạo danh sách lựa chọn loại phòng
    const categoryOptions = useMemo(
        () =>
            categories.map((item) => {
                const uniform = hasUniformPrice(item);

                const priceLabel = uniform
                    ? formatMoney(item.minCurrentPrice)
                    : 'Giá không đồng nhất hoặc chưa xác định';

                return {
                    value: String(item.categoryId),
                    label: `${item.categoryName} — ${priceLabel}`,
                    disabled: !uniform,
                };
            }),
        [categories]
    );

    const selectedCategory = categories.find(
        (item) => String(item.categoryId) === categoryId
    );

    const hasUsableCategories = categories.some(
        hasUniformPrice
    );

    // Gửi yêu cầu tạo kế hoạch
    async function handleSubmit() {
        setFormError(null);
        setCreatedPlan(null);

        if (!title.trim() || title.trim().length > 150) {
            setFormError(
                'Tên kế hoạch không được trống và tối đa 150 ký tự.'
            );
            return;
        }

        if (
            !selectedCategory ||
            !hasUniformPrice(selectedCategory)
        ) {
            setFormError(
                'Vui lòng chọn loại phòng có giá hiện tại đồng nhất.'
            );
            return;
        }

        const price = Number(newPrice);

        const decimalPlaces = String(newPrice).includes('.')
            ? String(newPrice).split('.')[1].length
            : 0;

        if (
            !Number.isFinite(price) ||
            price <= 0 ||
            price > 9999999999.99 ||
            decimalPlaces > 2
        ) {
            setFormError(
                'Giá đề xuất phải lớn hơn 0 và tối đa 2 chữ số thập phân.'
            );
            return;
        }

        if (!effectiveFrom) {
            setFormError('Vui lòng chọn ngày bắt đầu.');
            return;
        }

        if (effectiveTo && effectiveTo < effectiveFrom) {
            setFormError(
                'Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.'
            );
            return;
        }

        if (!reason.trim() || reason.trim().length > 500) {
            setFormError(
                'Lý do không được trống và tối đa 500 ký tự.'
            );
            return;
        }

        const request: CreatePricePlanRequest = {
            title: title.trim(),
            categoryId: selectedCategory.categoryId,
            newPrice: price,
            effectiveFrom,
            effectiveTo: effectiveTo || null,
            reason: reason.trim(),
        };

        setSubmitting(true);

        try {
            const result = await createPricePlan(request);

            setCreatedPlan(result);
        } catch (error: unknown) {
            setFormError(getErrorMessage(error));
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <Stack gap="lg">
            <Box>
                <Title order={2}>
                    Lập kế hoạch điều chỉnh giá
                </Title>

                <Text size="sm" c="dimmed" mt={5}>
                    US-09: Manager đề xuất thay đổi giá phòng
                    theo từng loại phòng.
                </Text>
            </Box>

            <Alert color="blue" title="Lưu ý nghiệp vụ">
                Khi tạo kế hoạch, hệ thống chỉ lưu giá đề xuất
                với trạng thái Draft. Giá phòng thực tế chưa
                thay đổi cho đến khi kế hoạch được duyệt và
                áp dụng theo nghiệp vụ tiếp theo.
            </Alert>

            {categoriesLoading && (
                <Paper withBorder radius="sm" p="md">
                    <Group>
                        <Loader size="sm" />
                        <Text size="sm" c="dimmed">
                            Đang tải danh sách loại phòng...
                        </Text>
                    </Group>
                </Paper>
            )}

            {categoryError && (
                <Alert color="red" title="Không thể tải loại phòng">
                    {categoryError}
                </Alert>
            )}

            {!categoriesLoading &&
                !categoryError &&
                categories.length === 0 && (
                    <Alert color="yellow" title="Chưa có dữ liệu">
                        API Occupancy chưa trả về danh sách loại phòng.
                    </Alert>
                )}

            <Paper withBorder radius="sm" p="lg">
                <Stack gap="md">
                    <Title order={4}>
                        Thông tin kế hoạch
                    </Title>

                    <TextInput
                        label="Tên kế hoạch"
                        placeholder="Ví dụ: Điều chỉnh giá Deluxe cuối năm"
                        value={title}
                        onChange={(event) =>
                            setTitle(event.currentTarget.value)
                        }
                        required
                        maxLength={150}
                    />

                    <Select
                        label="Loại phòng"
                        placeholder="Chọn loại phòng"
                        data={categoryOptions}
                        value={categoryId}
                        onChange={setCategoryId}
                        searchable
                        nothingFoundMessage="Không tìm thấy loại phòng"
                        required
                    />

                    {selectedCategory && (
                        hasUniformPrice(selectedCategory) ? (
                            <Paper
                                withBorder
                                radius="sm"
                                p="md"
                                bg="var(--mantine-color-gray-light)"
                            >
                                <Group justify="space-between">
                                    <Box>
                                        <Text size="xs" c="dimmed">
                                            Loại phòng đã chọn
                                        </Text>

                                        <Text fw={600}>
                                            {selectedCategory.categoryName}
                                        </Text>
                                    </Box>

                                    <Box>
                                        <Text size="xs" c="dimmed">
                                            Giá hiện tại
                                        </Text>

                                        <Text fw={700}>
                                            {formatMoney(
                                                selectedCategory.minCurrentPrice
                                            )}
                                        </Text>
                                    </Box>
                                </Group>
                            </Paper>
                        ) : (
                            <Alert color="red">
                                Giá phòng trong loại này không đồng nhất.
                                Không thể lập kế hoạch cho đến khi giá
                                được chuẩn hóa.
                            </Alert>
                        )
                    )}

                    <NumberInput
                        label="Giá đề xuất mới (VND)"
                        placeholder="Nhập giá mới"
                        value={newPrice}
                        onChange={setNewPrice}
                        min={0.01}
                        max={9999999999.99}
                        decimalScale={2}
                        allowNegative={false}
                        required
                    />

                    <Group grow align="flex-start">
                        <TextInput
                            label="Ngày bắt đầu"
                            type="date"
                            value={effectiveFrom}
                            onChange={(event) =>
                                setEffectiveFrom(
                                    event.currentTarget.value
                                )
                            }
                            required
                        />

                        <TextInput
                            label="Ngày kết thúc"
                            type="date"
                            min={effectiveFrom || undefined}
                            value={effectiveTo}
                            onChange={(event) =>
                                setEffectiveTo(
                                    event.currentTarget.value
                                )
                            }
                        />
                    </Group>

                    <Textarea
                        label="Lý do điều chỉnh"
                        placeholder="Nhập lý do đề xuất thay đổi giá..."
                        value={reason}
                        onChange={(event) =>
                            setReason(event.currentTarget.value)
                        }
                        minRows={3}
                        maxRows={6}
                        required
                    />

                    <Text size="xs" c="dimmed">
                        Ngày kết thúc có thể để trống nếu kế hoạch
                        chưa xác định thời điểm kết thúc.
                    </Text>

                    {formError && (
                        <Alert
                            color="red"
                            title="Không thể tạo kế hoạch"
                        >
                            {formError}
                        </Alert>
                    )}

                    <Group justify="flex-end">
                        <Button
                            onClick={handleSubmit}
                            loading={submitting}
                            disabled={
                                categoriesLoading ||
                                !hasUsableCategories
                            }
                        >
                            Tạo kế hoạch
                        </Button>
                    </Group>
                </Stack>
            </Paper>

            {createdPlan && (
                <Paper
                    withBorder
                    radius="sm"
                    p="lg"
                >
                    <Group justify="space-between" mb="md">
                        <Box>
                            <Title order={4}>
                                Tạo kế hoạch thành công
                            </Title>

                            <Text size="xs" c="dimmed" mt={4}>
                                Mã kế hoạch: #{createdPlan.planId}
                            </Text>
                        </Box>

                        <Badge color="yellow" variant="light">
                            {createdPlan.status}
                        </Badge>
                    </Group>

                    <Stack gap="sm">
                        <Text fw={600}>
                            {createdPlan.title}
                        </Text>

                        <Group justify="space-between">
                            <Text size="sm" c="dimmed">
                                Giá hiện tại
                            </Text>

                            <Text fw={600}>
                                {formatMoney(createdPlan.oldPrice)}
                            </Text>
                        </Group>

                        <Group justify="space-between">
                            <Text size="sm" c="dimmed">
                                Giá đề xuất
                            </Text>

                            <Text fw={600} c="blue">
                                {formatMoney(createdPlan.newPrice)}
                            </Text>
                        </Group>

                        <Group justify="space-between">
                            <Text size="sm" c="dimmed">
                                Ngày bắt đầu
                            </Text>

                            <Text>
                                {createdPlan.effectiveFrom}
                            </Text>
                        </Group>

                        <Group justify="space-between">
                            <Text size="sm" c="dimmed">
                                Ngày kết thúc
                            </Text>

                            <Text>
                                {createdPlan.effectiveTo || 'Không xác định'}
                            </Text>
                        </Group>

                        <Group justify="space-between">
                            <Text size="sm" c="dimmed">
                                Người tạo
                            </Text>

                            <Text>
                                User ID: {createdPlan.createdBy}
                            </Text>
                        </Group>

                        <Group justify="space-between">
                            <Text size="sm" c="dimmed">
                                Thời gian tạo
                            </Text>

                            <Text size="sm">
                                {formatDateTime(createdPlan.createdAt)}
                            </Text>
                        </Group>

                        <Alert color="green" title="Đã lưu thành công">
                            Kế hoạch đã được lưu ở trạng thái Draft.
                            Giá phòng thực tế chưa bị thay đổi.
                        </Alert>
                    </Stack>
                </Paper>
            )}
        </Stack>
    );
}
