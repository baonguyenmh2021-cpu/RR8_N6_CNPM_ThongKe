
import type {
    MarketingStrategyStatus,
} from './MarketingStrategy';

export interface UpdateMarketingStrategyRequest {
    title: string;
    targetSegment: string | null;
    description: string | null;
    startDate: string;
    endDate: string;
    budget: number;
    actualSpend: number;
    discountId: number | null;
    expectedBookings: number | null;
}

export interface ChangeMarketingStrategyStatusRequest {
    status: MarketingStrategyStatus;
}
