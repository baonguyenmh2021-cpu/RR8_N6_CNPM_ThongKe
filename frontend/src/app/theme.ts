
import { createTheme } from '@mantine/core';

export const theme = createTheme({
    primaryColor: 'brand',
    primaryShade: 6,

    colors: {
        brand: [
            '#edf5fd',
            '#dbeafd',
            '#b8d8f8',
            '#8fc1f1',
            '#66a8e7',
            '#3c90dc',
            '#206bc4',
            '#19599f',
            '#14467c',
            '#0e3159',
        ],
    },

    fontFamily:
        'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',

    headings: {
        fontFamily:
            'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        fontWeight: '600',
    },

    defaultRadius: 'sm',
});
