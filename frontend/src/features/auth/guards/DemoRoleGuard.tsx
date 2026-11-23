import type { ReactNode } from 'react';

import { Navigate } from 'react-router-dom';

import { useDemoRole } from '../hooks/useDemoRole';

import type { DemoRole } from '../types/DemoRole';

type DemoRoleGuardProps = {
    children: ReactNode;
    allowedRoles: DemoRole[];
};

export default function DemoRoleGuard({
    children,
    allowedRoles,
}: DemoRoleGuardProps) {
    const { role } = useDemoRole();

    if (!allowedRoles.includes(role)) {
        return (
            <Navigate
                to="/revenue"
                replace
            />
        );
    }

    return <>{children}</>;
}