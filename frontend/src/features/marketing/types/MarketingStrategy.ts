
export interface CreateMarketingStrategyRequest {
    title: string;

    targetSegment: string | null;

    description: string | null;

    startDate: string;

    endDate: string;

    budget: number;

    discountId: number | null;

    expectedBookings: number | null;
}

export interface MarketingStrategyResponse {
    strategyId: number;

    title: string;

    targetSegment: string | null;

    description: string | null;

    startDate: string;

    endDate: string;

    budget: number | string;

    actualSpend: number | string;

    discountId: number | null;

    expectedBookings: number | null;

    status: string;

    createdBy: number;

    createdAt: string;

    updatedAt: string;
}


// US-13: Thông tin chiến lược trong danh sách
export type MarketingStrategyStatus =
    | 'Planned'
    | 'Running'
    | 'Finished'
    | 'Cancelled';

export interface MarketingStrategySummaryResponse {
    strategyId: number;

    title: string;

    targetSegment: string | null;

    startDate: string;

    endDate: string;

    budget: number | string;

    status: MarketingStrategyStatus;
}
