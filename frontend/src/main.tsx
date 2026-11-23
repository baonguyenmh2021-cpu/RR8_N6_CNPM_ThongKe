
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { MantineProvider } from '@mantine/core';

import '@mantine/core/styles.css';
import './styles/global.css';

import App from './App';
import { theme } from './app/theme';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MantineProvider
      theme={theme}
      defaultColorScheme="light"
    >
      <App />
    </MantineProvider>
  </StrictMode>
);
