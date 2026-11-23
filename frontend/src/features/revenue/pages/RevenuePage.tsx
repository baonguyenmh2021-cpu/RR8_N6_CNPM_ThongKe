
import {
    Paper,
    Stack,
    Text,
    Title,
} from '@mantine/core';

export default function RevenuePage() {
    return (
        <Stack gap="lg">
            <div>
                <Title order={2}>
                    Báo cáo doanh thu
                </Title>

                <Text size="sm" c="dimmed" mt={4}>
                    Theo dõi doanh thu và hoạt động kinh doanh
                    của Home Rent.
                </Text>
            </div>

            <Paper withBorder radius="sm" p="xl">
                <Stack
                    gap="sm"
                    align="center"
                    justify="center"
                    style={{ minHeight: 220 }}
                >
                    <Title order={4}>
                        Thống kê doanh thu
                    </Title>

                    <Text size="sm" c="dimmed" ta="center">
                        Chưa kết nối dữ liệu từ Spring Boot.
                        Các thống kê sẽ được hiển thị sau khi
                        tích hợp API US01–US04.
                    </Text>
                </Stack>
            </Paper>
        </Stack>
    );
}
