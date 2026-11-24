
import { Navigate } from 'react-router-dom';

import { useDemoRole } from '../../auth/hooks/useDemoRole';

import CreatePricePlanPage from './CreatePricePlanPage';
import AdminPricePlanPage from './AdminPricePlanPage';

export default function PricePlanRoute() {
    const { role } = useDemoRole();

    if (role === 'MANAGER') {
        return <CreatePricePlanPage />;
    }

    if (role === 'ADMIN') {
        return <AdminPricePlanPage />;
    }

    return <Navigate to="/revenue" replace />;
}
