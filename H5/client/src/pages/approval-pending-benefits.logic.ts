export interface PendingBenefitsState {
  available: boolean;
  dismissed: boolean;
}

export function shouldShowPendingBenefitsEntry(state: PendingBenefitsState) {
  return state.available && !state.dismissed;
}
