export interface BenefitsUserCardOption {
  cardId: string;
  defaultCard: boolean;
}

export interface BenefitsSignUserCardOption extends BenefitsUserCardOption {
  bankName: string;
  cardLastFour: string;
}

export interface BenefitsSignStatus {
  accountNo: string;
  signed: boolean;
  status: string;
  userSignId?: number | null;
  canApplySign?: boolean;
}

export interface BenefitsSignApplyParams {
  accountNo: string;
}

export interface BenefitsSignApplyResult {
  userSignId: number;
  applyTime: string;
  status: string;
}

export interface BenefitsSignConfirmParams {
  userSignId: number;
  verificationCode: string;
}

export interface BenefitsSignConfirmResult {
  userSignId: number;
  agreementNo: string;
  signed: boolean;
  status: string;
}

export type BenefitsActivationSignGateResult =
  | { type: "activate" }
  | {
    type: "sign-required";
    accountNo: string;
    bankName: string;
    maskedCardNo: string;
  }
  | { type: "no-card" };

export function resolveDefaultBenefitsUserCard<T extends BenefitsUserCardOption>(userCards: T[]) {
  return userCards.find((card) => card.defaultCard) ?? userCards[0] ?? null;
}

export function canActivateBenefits({
  applicationId,
  protocolReady,
  hasJointLoginToken,
}: {
  applicationId: string | null | undefined;
  protocolReady: boolean;
  hasJointLoginToken: boolean;
}) {
  return Boolean(applicationId) && protocolReady && hasJointLoginToken;
}

export function canStartBenefitsActivation({
  applicationId,
  hasJointLoginToken,
}: {
  applicationId: string | null | undefined;
  hasJointLoginToken: boolean;
}) {
  return Boolean(applicationId) && hasJointLoginToken;
}

export async function checkBenefitsActivationSignGate({
  userCard,
  getSignStatus,
}: {
  userCard: BenefitsSignUserCardOption | null;
  getSignStatus: (accountNo: string) => Promise<BenefitsSignStatus>;
}): Promise<BenefitsActivationSignGateResult> {
  if (!userCard?.cardId) {
    return { type: "no-card" };
  }

  const signStatus = await getSignStatus(userCard.cardId);
  if (isSigned(signStatus) || canReuseExistingSign(signStatus)) {
    return { type: "activate" };
  }

  return {
    type: "sign-required",
    accountNo: userCard.cardId,
    bankName: userCard.bankName,
    maskedCardNo: maskCardLastFour(userCard.cardLastFour),
  };
}

export async function applyBenefitsSign({
  accountNo,
  applySign,
}: {
  accountNo: string;
  applySign: (params: BenefitsSignApplyParams) => Promise<BenefitsSignApplyResult>;
}) {
  return applySign({ accountNo });
}

export async function confirmBenefitsSignAndActivate({
  userSignId,
  verificationCode,
  confirmSign,
  activate,
}: {
  userSignId: number;
  verificationCode: string;
  confirmSign: (params: BenefitsSignConfirmParams) => Promise<BenefitsSignConfirmResult>;
  activate: () => Promise<void>;
}) {
  const confirmResult = await confirmSign({ userSignId, verificationCode });
  if (!isSigned(confirmResult)) {
    throw new Error("签约确认失败，请重试");
  }
  await activate();
  return { type: "activated" as const };
}

function isSigned(status: { signed: boolean; status: string }) {
  return status.signed || status.status.toUpperCase() === "SIGNED";
}

function canReuseExistingSign(status: BenefitsSignStatus) {
  return Boolean(status.userSignId) && status.canApplySign === false;
}

function maskCardLastFour(cardLastFour: string) {
  return `**** ${cardLastFour || "****"}`;
}
