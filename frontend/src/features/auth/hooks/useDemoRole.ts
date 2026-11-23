import { useContext } from 'react';

import { DemoRoleContext } from '../context/DemoRoleContext';

export function useDemoRole() {
    const context = useContext(DemoRoleContext);

    if (!context) {
        throw new Error(
            'useDemoRole phải được sử dụng bên trong DemoRoleProvider'
        );
    }

    return context;
}