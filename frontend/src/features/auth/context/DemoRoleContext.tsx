import {
    createContext,
    useCallback,
    useMemo,
    useState,
    type ReactNode,
} from 'react';

import type { DemoRole } from '../types/DemoRole';

const STORAGE_KEY = 'rr8_demo_role';

export interface DemoRoleContextValue {
    role: DemoRole;
    setRole: (role: DemoRole) => void;
    isManager: boolean;
    isAdmin: boolean;
}

export const DemoRoleContext =
    createContext<DemoRoleContextValue | undefined>(undefined);

function getInitialRole(): DemoRole {
    try {
        const stored = sessionStorage.getItem(STORAGE_KEY);

        if (stored === 'MANAGER' || stored === 'ADMIN') {
            return stored;
        }
    } catch {
        // Nếu sessionStorage không khả dụng, dùng Manager.
    }

    return 'MANAGER';
}

export function DemoRoleProvider({
    children,
}: {
    children: ReactNode;
}) {
    const [role, setCurrentRole] = useState<DemoRole>(
        getInitialRole
    );

    const setRole = useCallback((nextRole: DemoRole) => {
        setCurrentRole(nextRole);

        try {
            sessionStorage.setItem(STORAGE_KEY, nextRole);
        } catch {
            // Vẫn chuyển vai trò trong phiên hiện tại.
        }
    }, []);

    const value = useMemo(
        () => ({
            role,
            setRole,
            isManager: role === 'MANAGER',
            isAdmin: role === 'ADMIN',
        }),
        [role, setRole]
    );

    return (
        <DemoRoleContext.Provider value={value}>
            {children}
        </DemoRoleContext.Provider>
    );
}