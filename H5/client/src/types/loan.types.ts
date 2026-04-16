export interface ApiResponse<T> {
  code: number;
  data: T;
  message?: string;
}

export interface AmountRange {
  min: number;
  max: number;
  step: number;
  default: number;
}

export interface TermOption {
  label: string;
  value: number;
}

export interface BankAccount {
  bankName: string;
  lastFour: string;
  accountId?: string;
}

export interface CalculatorConfig {
  amountRange: AmountRange;
  termOptions: TermOption[];
  annualRate: number;
  lender: string;
  receivingAccount: BankAccount;
}

export interface RepaymentPlanItem {
  period: number;
  date: string;
  principal: number;
  interest: number;
  total: number;
}

export interface CalculateParams {
  amount: number;
  term: number;
}

export interface CalculateResult {
  totalFee: number;
  annualRate: string;
  repaymentPlan: RepaymentPlanItem[];
}

export interface ApplyParams {
  amount: number;
  term: number;
  receivingAccountId: string;
  agreedProtocols: string[];
}

export interface ApplyResult {
  applicationId: string | null;
  status: "pending" | "loan_failed";
  estimatedTime: string;
  benefitsActivated: boolean;
  benefitOrderNo?: string | null;
  message?: string;
}

export type StepStatus = "completed" | "in_progress" | "pending";

export interface ApprovalStep {
  name: string;
  status: StepStatus;
  description: string;
}

export interface BenefitsCardPreview {
  available: boolean;
  price: number;
  features: string[];
}

export type ApprovalStatusValue = "pending" | "reviewing" | "approved" | "rejected";

export interface ApprovalStatus {
  applicationId: string;
  status: ApprovalStatusValue;
  steps: ApprovalStep[];
  benefitsCard: BenefitsCardPreview;
}

export interface BenefitFeature {
  title: string;
  description: string;
}

export interface BenefitItem {
  discount: string;
  title: string;
  description: string;
  validity: string;
  originalPrice: number;
  saving: number;
}

export interface BenefitCategory {
  name: string;
  icon: "tv" | "car" | "life" | "shop" | string;
  benefits: BenefitItem[];
}

export interface ProtocolLink {
  name: string;
  url: string;
}

export interface BenefitsCardDetail {
  cardName: string;
  price: number;
  totalSaving: number;
  features: BenefitFeature[];
  categories: BenefitCategory[];
  tips: string[];
  protocols: ProtocolLink[];
}

export interface ActivateParams {
  applicationId: string;
  cardType: string;
}

export interface ActivateResult {
  activationId: string;
  status: "activated" | "failed";
  message: string;
}

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

export interface RepaymentInfo {
  loanId: string;
  repaymentAmount: number;
  repaymentType: string;
  bankCard: BankAccount;
  tip: string;
}

export interface RepaymentParams {
  loanId: string;
  amount: number;
  bankCardId: string;
  repaymentType: "early" | "scheduled";
}

export interface RepaymentSubmitResult {
  repaymentId: string;
  status: "processing" | "failed";
  message: string;
}

export interface RepaymentResult {
  repaymentId: string;
  status: "success" | "failed";
  amount: number;
  repaymentTime: string;
  bankCard: BankAccount;
  interestSaved: number;
  tips: string[];
}

export interface LoginParams {
  phone: string;
  code: string;
}

export interface LoginResult {
  token: string;
}

export type JointLoginScene = "push" | "exercise" | "refund";

export interface JointLoginParams {
  token: string;
  scene: JointLoginScene;
  orderNo?: string;
  benefitOrderNo?: string;
  productCode?: string;
}

export interface JointLoginResult {
  loginSuccess: boolean;
  scene: string;
  targetPage: "joint-dispatch" | "joint-refund-entry" | "joint-unsupported";
  benefitOrderNo: string | null;
  localUserReady: boolean;
}
