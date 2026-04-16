import type { Locale } from "./locale";

export interface BenefitFeatureCard {
  title: string;
  desc: string;
}

export interface BenefitItem {
  discount: string;
  title: string;
  desc: string;
  original: string;
  save: string;
}

const approvalPendingFeatures: Record<Locale, string[]> = {
  "zh-CN": ["热门影音会员，出行礼包", "享最高300元优惠", "专属优先通道，不成功不收费"],
  "zh-TW": ["熱門影音會員，出行禮包", "享最高300元優惠", "專屬優先通道，不成功不收費"],
  "en-US": ["Popular streaming memberships and travel perks", "Up to ¥300 discount", "Priority channel, no fee if unsuccessful"],
  "vi-VN": ["Gói hội viên giải trí và ưu đãi di chuyển", "Ưu đãi lên tới ¥300", "Kênh ưu tiên, không thành công không thu phí"],
};

const benefitsFeatureCards: Record<Locale, BenefitFeatureCard[]> = {
  "zh-CN": [
    { title: "先享后付，无压力消费", desc: "支持多种消费场景，先享受服务后付款，让您的消费更灵活" },
    { title: "费用预估¥100/月，共3期", desc: "分期付款，减轻还款压力，每月仅需100元，轻松管理财务" },
    { title: "匹配优质权益，不成功不收费", desc: "智能匹配最适合您的金融产品，申请不成功不收取任何费用" },
  ],
  "zh-TW": [
    { title: "先享後付，無壓力消費", desc: "支持多種消費場景，先享受服務後付款，讓您的消費更靈活" },
    { title: "費用預估¥100/月，共3期", desc: "分期付款，減輕還款壓力，每月僅需100元，輕鬆管理財務" },
    { title: "匹配優質權益，不成功不收費", desc: "智能匹配最適合您的金融產品，申請不成功不收取任何費用" },
  ],
  "en-US": [
    { title: "Enjoy first, pay later", desc: "Use benefits in multiple scenarios and pay later for more flexibility." },
    { title: "Estimated fee ¥100/month for 3 months", desc: "Installment payment helps reduce monthly repayment pressure." },
    { title: "Matched premium benefits, no fee if unsuccessful", desc: "Smart matching helps you access suitable financial products with no charge if unsuccessful." },
  ],
  "vi-VN": [
    { title: "Dùng trước, trả sau", desc: "Sử dụng trong nhiều tình huống khác nhau và thanh toán sau để linh hoạt hơn." },
    { title: "Ước tính phí ¥100/tháng trong 3 tháng", desc: "Thanh toán theo kỳ giúp giảm áp lực trả nợ hằng tháng." },
    { title: "Ghép quyền lợi chất lượng, không thành công không mất phí", desc: "Hệ thống sẽ đề xuất sản phẩm phù hợp và không thu phí nếu không thành công." },
  ],
};

const benefitsData: Record<Locale, BenefitItem[][]> = {
  "zh-CN": [
    [
      { discount: "5折", title: "每月4选1影视VIP会员", desc: "腾讯视频、优酷会员、爱奇艺、芒果TV任选其一\n有效期30天/次", original: "30元", save: "省15元" },
      { discount: "4.5折", title: "每月2选1音乐VIP会员", desc: "QQ音乐豪华绿钻、网易云音乐黑胶VIP任选\n有效期30天/次", original: "29元", save: "省11元" },
    ],
    [
      { discount: "6折", title: "滴滴出行优惠券", desc: "每月10张滴滴出行优惠券\n有效期30天/次", original: "50元", save: "省20元" },
      { discount: "7折", title: "高铁/机票优惠", desc: "每月享受高铁/机票折扣\n有效期30天/次", original: "100元", save: "省30元" },
    ],
    [
      { discount: "5折", title: "美团外卖红包", desc: "每月20张外卖红包\n有效期30天/次", original: "40元", save: "省20元" },
      { discount: "6折", title: "超市购物优惠", desc: "每月享受超市购物折扣\n有效期30天/次", original: "60元", save: "省24元" },
    ],
    [
      { discount: "8折", title: "京东购物券", desc: "每月5张京东购物券\n有效期30天/次", original: "50元", save: "省10元" },
      { discount: "7折", title: "淘宝/天猫优惠", desc: "每月享受淘宝/天猫折扣\n有效期30天/次", original: "80元", save: "省24元" },
    ],
  ],
  "zh-TW": [
    [
      { discount: "5折", title: "每月4選1影視VIP會員", desc: "騰訊視頻、優酷會員、愛奇藝、芒果TV任選其一\n有效期30天/次", original: "30元", save: "省15元" },
      { discount: "4.5折", title: "每月2選1音樂VIP會員", desc: "QQ音樂豪華綠鑽、網易雲音樂黑膠VIP任選\n有效期30天/次", original: "29元", save: "省11元" },
    ],
    [
      { discount: "6折", title: "滴滴出行優惠券", desc: "每月10張滴滴出行優惠券\n有效期30天/次", original: "50元", save: "省20元" },
      { discount: "7折", title: "高鐵/機票優惠", desc: "每月享受高鐵/機票折扣\n有效期30天/次", original: "100元", save: "省30元" },
    ],
    [
      { discount: "5折", title: "美團外賣紅包", desc: "每月20張外賣紅包\n有效期30天/次", original: "40元", save: "省20元" },
      { discount: "6折", title: "超市購物優惠", desc: "每月享受超市購物折扣\n有效期30天/次", original: "60元", save: "省24元" },
    ],
    [
      { discount: "8折", title: "京東購物券", desc: "每月5張京東購物券\n有效期30天/次", original: "50元", save: "省10元" },
      { discount: "7折", title: "淘寶/天貓優惠", desc: "每月享受淘寶/天貓折扣\n有效期30天/次", original: "80元", save: "省24元" },
    ],
  ],
  "en-US": [
    [
      { discount: "50%", title: "Choose 1 of 4 streaming VIP passes each month", desc: "Tencent Video, Youku, iQIYI or Mango TV\nValid for 30 days each time", original: "¥30", save: "Save ¥15" },
      { discount: "45%", title: "Choose 1 of 2 music VIP passes each month", desc: "QQ Music Luxury VIP or NetEase Cloud Music Black Vinyl VIP\nValid for 30 days each time", original: "¥29", save: "Save ¥11" },
    ],
    [
      { discount: "60%", title: "Didi ride coupons", desc: "10 Didi coupons per month\nValid for 30 days each time", original: "¥50", save: "Save ¥20" },
      { discount: "70%", title: "High-speed rail / flight discount", desc: "Travel discounts every month\nValid for 30 days each time", original: "¥100", save: "Save ¥30" },
    ],
    [
      { discount: "50%", title: "Meituan food delivery coupons", desc: "20 food delivery coupons per month\nValid for 30 days each time", original: "¥40", save: "Save ¥20" },
      { discount: "60%", title: "Supermarket shopping discount", desc: "Monthly supermarket shopping discounts\nValid for 30 days each time", original: "¥60", save: "Save ¥24" },
    ],
    [
      { discount: "80%", title: "JD shopping coupons", desc: "5 JD coupons per month\nValid for 30 days each time", original: "¥50", save: "Save ¥10" },
      { discount: "70%", title: "Taobao / Tmall shopping discount", desc: "Monthly Taobao / Tmall discounts\nValid for 30 days each time", original: "¥80", save: "Save ¥24" },
    ],
  ],
  "vi-VN": [
    [
      { discount: "50%", title: "Chọn 1 trong 4 gói VIP xem phim mỗi tháng", desc: "Tencent Video, Youku, iQIYI hoặc Mango TV\nHiệu lực 30 ngày/lần", original: "¥30", save: "Tiết kiệm ¥15" },
      { discount: "45%", title: "Chọn 1 trong 2 gói VIP âm nhạc mỗi tháng", desc: "QQ Music Luxury VIP hoặc NetEase Cloud Music Black Vinyl VIP\nHiệu lực 30 ngày/lần", original: "¥29", save: "Tiết kiệm ¥11" },
    ],
    [
      { discount: "60%", title: "Phiếu ưu đãi đi xe Didi", desc: "10 phiếu Didi mỗi tháng\nHiệu lực 30 ngày/lần", original: "¥50", save: "Tiết kiệm ¥20" },
      { discount: "70%", title: "Ưu đãi tàu cao tốc / vé máy bay", desc: "Ưu đãi di chuyển hàng tháng\nHiệu lực 30 ngày/lần", original: "¥100", save: "Tiết kiệm ¥30" },
    ],
    [
      { discount: "50%", title: "Phiếu giảm giá giao đồ ăn Meituan", desc: "20 phiếu giao đồ ăn mỗi tháng\nHiệu lực 30 ngày/lần", original: "¥40", save: "Tiết kiệm ¥20" },
      { discount: "60%", title: "Ưu đãi mua sắm siêu thị", desc: "Ưu đãi siêu thị mỗi tháng\nHiệu lực 30 ngày/lần", original: "¥60", save: "Tiết kiệm ¥24" },
    ],
    [
      { discount: "80%", title: "Phiếu mua sắm JD", desc: "5 phiếu JD mỗi tháng\nHiệu lực 30 ngày/lần", original: "¥50", save: "Tiết kiệm ¥10" },
      { discount: "70%", title: "Ưu đãi Taobao / Tmall", desc: "Ưu đãi mua sắm mỗi tháng\nHiệu lực 30 ngày/lần", original: "¥80", save: "Tiết kiệm ¥24" },
    ],
  ],
};

const benefitsTips: Record<Locale, string[]> = {
  "zh-CN": [
    "权益服务费基于惠聚会员服务收取，包含影音娱乐、购物出行、生活服务、放款通道匹配等特权。",
    "本费用为互联网增值服务，独立于借款，非贷款利率、保障金或违约金。",
    "您充分知晓服务内容，先享后付模式下系统将从您指定银行卡扣费。",
  ],
  "zh-TW": [
    "權益服務費基於惠聚會員服務收取，包含影音娛樂、購物出行、生活服務、放款通道匹配等特權。",
    "本費用為互聯網增值服務，獨立於借款，非貸款利率、保證金或違約金。",
    "您充分知曉服務內容，先享後付模式下系統將從您指定銀行卡扣費。",
  ],
  "en-US": [
    "The service fee covers member benefits including entertainment, shopping, travel and channel matching.",
    "This is an internet value-added service fee and is independent of the loan.",
    "By activating first-use-then-pay, the system will charge your designated bank card.",
  ],
  "vi-VN": [
    "Phí dịch vụ bao gồm các quyền lợi thành viên như giải trí, mua sắm, di chuyển và ghép kênh giải ngân.",
    "Đây là phí dịch vụ giá trị gia tăng trên internet, độc lập với khoản vay.",
    "Khi kích hoạt dùng trước trả sau, hệ thống sẽ khấu trừ từ thẻ ngân hàng đã chỉ định.",
  ],
};

const repaymentSuccessTips: Record<Locale, string[]> = {
  "zh-CN": [
    "还款金额已从您的银行卡扣除，请注意查收银行通知",
    "提前还款后，您的信用额度将即时恢复",
    "如需查看还款记录，可前往\"我的\"-\"账单明细\"",
    "若有任何疑问，请联系客服：400-888-8888",
  ],
  "zh-TW": [
    "還款金額已從您的銀行卡扣除，請注意查收銀行通知",
    "提前還款後，您的信用額度將即時恢復",
    "如需查看還款記錄，可前往\"我的\"-\"帳單明細\"",
    "若有任何疑問，請聯繫客服：400-888-8888",
  ],
  "en-US": [
    "The repayment amount has been deducted from your bank card.",
    "Your credit limit will recover immediately after early repayment.",
    "You can view repayment records in My Account > Bills.",
    "If you have any questions, please contact customer service: 400-888-8888.",
  ],
  "vi-VN": [
    "Khoản tiền trả nợ đã được khấu trừ từ thẻ ngân hàng của bạn.",
    "Hạn mức tín dụng sẽ được khôi phục ngay sau khi trả trước hạn.",
    "Bạn có thể xem lịch sử trả nợ tại Tài khoản của tôi > Sao kê.",
    "Nếu có thắc mắc, vui lòng liên hệ CSKH: 400-888-8888.",
  ],
};

export function getApprovalPendingFeatures(locale: Locale) {
  return approvalPendingFeatures[locale];
}

export function getBenefitsFeatureCards(locale: Locale) {
  return benefitsFeatureCards[locale];
}

export function getBenefitsData(locale: Locale, tabIndex: number) {
  return benefitsData[locale][tabIndex] ?? [];
}

export function getBenefitsTips(locale: Locale) {
  return benefitsTips[locale];
}

export function getRepaymentSuccessTips(locale: Locale) {
  return repaymentSuccessTips[locale];
}

const jointDispatchTips: Record<Locale, string[]> = {
  "zh-CN": [
    "当前已完成联合登录入口校验，后续可按场景继续分发到供应商页面。",
    "如需正式跳转齐为供应商页面，仍需补齐真实跳转地址与参数口径。",
    "当前页面先作为分发页占位，用于联调登录态与场景恢复。",
  ],
  "zh-TW": [
    "目前已完成聯合登入入口校驗，後續可按場景繼續分發到供應商頁面。",
    "如需正式跳轉齊為供應商頁面，仍需補齊真實跳轉地址與參數口徑。",
    "目前頁面先作為分發頁佔位，用於聯調登入態與場景恢復。",
  ],
  "en-US": [
    "Joint login entry validation has completed and this page now acts as the dispatch placeholder.",
    "A real supplier redirect still requires the final QW target URL and parameter contract.",
    "Use this page to verify login state and scene recovery during integration.",
  ],
  "vi-VN": [
    "Bước kiểm tra đầu vào đăng nhập liên hợp đã hoàn tất và trang này đóng vai trò trang phân phát tạm thời.",
    "Để chuyển hướng thực tới nhà cung cấp, vẫn cần URL và hợp đồng tham số cuối cùng từ QW.",
    "Hãy dùng trang này để kiểm tra trạng thái đăng nhập và khôi phục ngữ cảnh khi liên kết thử nghiệm.",
  ],
};

const jointRefundNotes: Record<Locale, string[]> = {
  "zh-CN": [
    "当前页面仅作为退款入口承接页，占位验证联合登录与场景识别。",
    "退款申请、退款结果查询与退款进度展示，待外部文档明确后继续扩展。",
    "如后续需支持正式退款流程，应补齐接口、状态流转和异常页设计。",
  ],
  "zh-TW": [
    "目前頁面僅作為退款入口承接頁，用於驗證聯合登入與場景識別。",
    "退款申請、退款結果查詢與退款進度展示，待外部文件明確後再繼續擴展。",
    "如後續需支援正式退款流程，應補齊介面、狀態流轉與異常頁設計。",
  ],
  "en-US": [
    "This page currently serves as the refund-entry placeholder for joint-login verification.",
    "Refund application, result query, and progress display will be extended after the external contract is finalized.",
    "A production refund flow will still need APIs, status transitions, and dedicated exception handling.",
  ],
  "vi-VN": [
    "Trang này hiện chỉ đóng vai trò trang tiếp nhận đầu vào hoàn tiền để kiểm tra đăng nhập liên hợp.",
    "Yêu cầu hoàn tiền, tra cứu kết quả và hiển thị tiến độ sẽ được mở rộng sau khi hợp đồng bên ngoài được chốt.",
    "Quy trình hoàn tiền chính thức vẫn cần API, luồng trạng thái và thiết kế trang lỗi riêng.",
  ],
};

export function getJointDispatchTips(locale: Locale) {
  return jointDispatchTips[locale];
}

export function getJointRefundNotes(locale: Locale) {
  return jointRefundNotes[locale];
}
