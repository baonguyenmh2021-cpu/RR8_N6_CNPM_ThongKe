
export interface MarketingEffectivenessResponse {
    strategyId: number;
    title: string;
    status: string;

    startDate: string;
    endDate: string;

    budget: number | string;
    actualSpend: number | string;

    discountId: number | null;
    expectedBookings: number | null;

    revenueBefore: number | string;
    revenueDuring: number | string;

    bookingsBefore: number;
    bookingsDuring: number;

    revenueChangePercent: number | string | null;
    bookingAchievementPercent: number | string | null;

    bookingsWithCode: number;
}
