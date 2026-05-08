export interface BenefitsUserCardOption {
  cardId: string;
  defaultCard: boolean;
}

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
