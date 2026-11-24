
export interface ReviewPricePlanRequest {
    approve: boolean;
    rejectReason: string | null;
}

export interface ReviewPricePlanResponse {
    planId: number;
    title: string;
    categoryId: number;
    effectiveFrom: string;
    effectiveTo: string | null;
    status: string;
    createdBy: number;
    approvedBy: number | null;
    approvedAt: string | null;
    rejectReason: string | null;
}
