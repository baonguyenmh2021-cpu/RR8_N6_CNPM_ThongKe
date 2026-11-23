
export type DiscountType = 'PERCENT' | 'FIXED';

// Dữ liệu gửi từ React tới Spring Boot
export interface CreateDiscountRequest {
  code: string;
  description?: string | null;

  type: DiscountType;
  value: number;
  minAmount?: number | null;

  validFrom: string;
  validTo: string;

  usageLimit?: number | null;
}

// Dữ liệu Spring Boot trả về sau khi tạo
export interface DiscountResponse {
  discountId: number;
  code: string;
  description: string | null;

  type: DiscountType;
  value: number | string;
  minAmount: number | string;

  validFrom: string;
  validTo: string;

  usageLimit: number | null;
  status: string;

  createdBy: number;
  createdAt: string;
}
