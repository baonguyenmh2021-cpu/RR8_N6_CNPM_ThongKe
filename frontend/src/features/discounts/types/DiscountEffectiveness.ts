
export interface DiscountEffectivenessResponse {
    discountId: number;
    code: string;
    effectiveStatus: string;

    usageLimit: number | null;
    uses: number;
    remainingUses: number | null;

    totalDiscount: number | string;
    netRevenue: number | string;
}
