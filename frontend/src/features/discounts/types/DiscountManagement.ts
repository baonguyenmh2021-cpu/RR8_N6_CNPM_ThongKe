
export interface DiscountEligibilityResponse {
    discountId: number;
    code: string;
    storedStatus: string;
    effectiveStatus: string;
    validFrom: string;
    validTo: string;
    minAmount: number | string;
    orderAmount: number | string;
    usageLimit: number | null;
    uses: number;
    remainingUses: number | null;
    canApply: boolean;
    reason: string;
}

export interface ExtendDiscountRequest {
    newValidTo: string;
}

export interface DiscountStateResponse {
    discountId: number;
    code: string;
    storedStatus: string;
    validFrom: string;
    validTo: string;
    minAmount: number | string;
    usageLimit: number | null;
    uses: number;
    databaseToday: string;
}
