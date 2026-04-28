export const LOAN_PURPOSE_KEYS = [
  "calculator.loanPurpose.shopping",
  "calculator.loanPurpose.rent",
  "calculator.loanPurpose.education",
  "calculator.loanPurpose.travel",
] as const;

export const PROTOCOL_KEYS = [
  "calculator.protocol.loanContract",
  "calculator.protocol.privacyAuth",
  "calculator.protocol.userService",
  "calculator.protocol.privacy",
  "calculator.protocol.payment",
] as const;

export type ProtocolKey = (typeof PROTOCOL_KEYS)[number];

export const PARTNERS = [
  { short: "海尔", full: "海尔消费金融", color: "#3d8aff" },
  { short: "长银", full: "长银消费金融", color: "#ff6b6b" },
  { short: "小米", full: "小米消费金融", color: "#4b93ff" },
  { short: "哈银", full: "哈银消费金融", color: "#ff8c42" },
  { short: "中信", full: "中信消费金融", color: "#e63946" },
  { short: "蒙商", full: "蒙商消金", color: "#3dafff" },
  { short: "本溪", full: "本溪银行", color: "#d62828" },
  { short: "众邦", full: "众邦银行", color: "#165dff" },
] as const;
