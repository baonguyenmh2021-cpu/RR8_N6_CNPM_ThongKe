
export interface CreatePricePlanRequest {
    title: string;
    categoryId: number;
    newPrice: number;
    effectiveFrom: string;
    effectiveTo: string | null;
    reason: string;
}

export interface PricePlanResponse {
    planId: number;
    title: string;
    categoryId: number;
    oldPrice: number | string;
    newPrice: number | string;
    effectiveFrom: string;
    effectiveTo: string | null;
    reason: string;
    status: string;
    createdBy: number;
    createdAt: string;
}
