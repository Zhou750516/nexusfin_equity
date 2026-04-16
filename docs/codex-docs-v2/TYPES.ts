/**
 * TYPES.ts — H5 前端 TypeScript 类型定义
 *
 * 使用方式:
 *   复制到 H5/client/src/types/loan.types.ts
 *   导入: import type { CalculatorConfig, ... } from "@/types/loan.types"
 *
 * 对应 Java 后端:
 *   每个 interface 对应 src/main/java/.../dto/ 下的 DTO 类
 *   字段命名: TypeScript camelCase ↔ Java camelCase（Jackson 默认）
 */

// ============================================================
// 通用 API 响应封装
// Java: ApiResult<T>
// ============================================================

export interface ApiResponse<T> {
  code: number;        // 0 = 成功, 非零 = 错误
  data: T;
  message?: string;    // code !== 0 时的错误描述
}

// ============================================================
// 试算页相关类型
// Java Controller: LoanController
// ============================================================

/** Java DTO: AmountRangeDTO */
export interface AmountRange {
  min: number;         // 最小金额, e.g., 100
  max: number;         // 最大金额, e.g., 5000
  step: number;        // 步进, e.g., 100
  default: number;     // 默认金额, e.g., 3000
}

/** Java DTO: TermOptionDTO */
export interface TermOption {
  label: string;       // 显示文本, e.g., "3期"
  value: number;       // 期数值, e.g., 3
}

/** Java DTO: BankAccountDTO */
export interface BankAccount {
  bankName: string;    // 银行名称, e.g., "招商银行"
  lastFour: string;    // 卡号后四位, e.g., "8648"
  accountId?: string;  // 账户ID, e.g., "acc_001"
}

/** Java DTO: CalculatorConfigDTO — GET /api/loan/calculator-config */
export interface CalculatorConfig {
  amountRange: AmountRange;
  termOptions: TermOption[];
  annualRate: number;
  lender: string;
  receivingAccount: BankAccount;
}

/** Java DTO: RepaymentPlanItemDTO */
export interface RepaymentPlanItem {
  period: number;      // 期数 (1-based)
  date: string;        // ISO 日期: "2026-05-07"
  principal: number;   // 本金
  interest: number;    // 利息
  total: number;       // 本期合计
}

/** Java DTO: CalculateParamsDTO — POST /api/loan/calculate 请求体 */
export interface CalculateParams {
  amount: number;
  term: number;
}

/** Java DTO: CalculateResultDTO — POST /api/loan/calculate 响应 */
export interface CalculateResult {
  totalFee: number;
  annualRate: string;
  repaymentPlan: RepaymentPlanItem[];
}

// ============================================================
// 借款申请相关类型
// Java Controller: LoanController
// ============================================================

/** Java DTO: ApplyParamsDTO — POST /api/loan/apply 请求体 */
export interface ApplyParams {
  amount: number;
  term: number;
  receivingAccountId: string;
  agreedProtocols: string[];
}

/** Java DTO: ApplyResultDTO — POST /api/loan/apply 响应 */
export interface ApplyResult {
  applicationId: string;
  status: "pending";
  estimatedTime: string;
}

// ============================================================
// 审批状态相关类型
// Java Controller: LoanController
// ============================================================

export type StepStatus = "completed" | "in_progress" | "pending";

/** Java DTO: ApprovalStepDTO */
export interface ApprovalStep {
  name: string;
  status: StepStatus;
  description: string;
}

/** Java DTO: BenefitsCardPreviewDTO */
export interface BenefitsCardPreview {
  available: boolean;
  price: number;
  features: string[];
}

export type ApprovalStatusValue = "pending" | "reviewing" | "approved" | "rejected";

/** Java DTO: ApprovalStatusDTO — GET /api/loan/approval-status/{id} 响应 */
export interface ApprovalStatus {
  applicationId: string;
  status: ApprovalStatusValue;
  steps: ApprovalStep[];
  benefitsCard: BenefitsCardPreview;
}

// ============================================================
// 惠选卡相关类型
// Java Controller: BenefitsController
// ============================================================

/** Java DTO: BenefitFeatureDTO */
export interface BenefitFeature {
  title: string;
  description: string;
}

/** Java DTO: BenefitItemDTO */
export interface BenefitItem {
  discount: string;
  title: string;
  description: string;
  validity: string;
  originalPrice: number;
  saving: number;
}

/** Java DTO: BenefitCategoryDTO */
export interface BenefitCategory {
  name: string;
  icon: "tv" | "car" | "life" | "shop";
  benefits: BenefitItem[];
}

/** Java DTO: ProtocolLinkDTO */
export interface ProtocolLink {
  name: string;
  url: string;
}

/** Java DTO: BenefitsCardDetailDTO — GET /api/benefits/card-detail 响应 */
export interface BenefitsCardDetail {
  cardName: string;
  price: number;
  totalSaving: number;
  features: BenefitFeature[];
  categories: BenefitCategory[];
  tips: string[];
  protocols: ProtocolLink[];
}

/** Java DTO: ActivateParamsDTO — POST /api/benefits/activate 请求体 */
export interface ActivateParams {
  applicationId: string;
  cardType: string;
}

/** Java DTO: ActivateResultDTO — POST /api/benefits/activate 响应 */
export interface ActivateResult {
  activationId: string;
  status: "activated" | "failed";
  message: string;
}

// ============================================================
// 审批结果相关类型
// Java Controller: LoanController
// ============================================================

/** Java DTO: ApprovalResultDTO — GET /api/loan/approval-result/{id} 响应 */
export interface ApprovalResult {
  applicationId: string;
  status: "approved" | "rejected";
  approvedAmount: number;
  estimatedArrivalTime: string;
  steps: ApprovalStep[];
  benefitsCardActivated: boolean;
  tip: string;
  loanId: string | null;
}

// ============================================================
// 还款相关类型
// Java Controller: RepaymentController
// ============================================================

/** Java DTO: RepaymentInfoDTO — GET /api/repayment/info/{loanId} 响应 */
export interface RepaymentInfo {
  loanId: string;
  repaymentAmount: number;
  repaymentType: string;
  bankCard: BankAccount;
  tip: string;
}

/** Java DTO: RepaymentParamsDTO — POST /api/repayment/submit 请求体 */
export interface RepaymentParams {
  loanId: string;
  amount: number;
  bankCardId: string;
  repaymentType: "early" | "scheduled";
}

/** Java DTO: RepaymentSubmitResultDTO — POST /api/repayment/submit 响应 */
export interface RepaymentSubmitResult {
  repaymentId: string;
  status: "processing" | "failed";
  message: string;
}

/** Java DTO: RepaymentResultDTO — GET /api/repayment/result/{id} 响应 */
export interface RepaymentResult {
  repaymentId: string;
  status: "success" | "failed";
  amount: number;
  repaymentTime: string;
  bankCard: BankAccount;
  interestSaved: number;
  tips: string[];
}

// ============================================================
// 用户认证相关类型 (Phase 10)
// Java Controller: AuthController
// ============================================================

/** Java DTO: LoginParamsDTO — POST /api/auth/login 请求体 */
export interface LoginParams {
  phone: string;
  code: string;
}

/** Java DTO: LoginResultDTO — POST /api/auth/login 响应 */
export interface LoginResult {
  token: string;
  expiresIn: number;
  user: UserProfile;
}

/** Java DTO: UserProfileDTO */
export interface UserProfile {
  userId: string;
  phone: string;
  realName?: string;
  verified: boolean;
}
