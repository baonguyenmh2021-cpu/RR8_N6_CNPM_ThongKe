
export type PricePlanExecutionAction = 'apply' | 'end';

export interface PricePlanExecutionResponse {
    planId: number;
    categoryId: number;
    oldPrice: number | string;
    newPrice: number | string;
    effectiveFrom: string;
    effectiveTo: string | null;
    status: string;
    appliedAt: string | null;
}
