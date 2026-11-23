
import {
    Paper,
    Stack,
    Text,
    Title,
} from '@mantine/core';

type ComingSoonPageProps = {
    title: string;
    description?: string;
};

export default function ComingSoonPage({
    title,
    description = 'Giao diện chức năng này đang được triển khai. Chưa kết nối API backend.',
}: ComingSoonPageProps) {
    return (
        <Stack gap="lg">
            <Title order={2}>{title}</Title>

            <Paper withBorder radius="sm" p="xl">
                <Text c="dimmed" size="sm">
                    {description}
                </Text>
            </Paper>
        </Stack>
    );
}
