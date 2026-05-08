export interface PendingBenefitsState {
  available: boolean;
  dismissed: boolean;
  hasJointLoginToken: boolean;
}

export function shouldShowPendingBenefitsEntry(state: PendingBenefitsState) {
  return state.available && !state.dismissed && state.hasJointLoginToken;
}
