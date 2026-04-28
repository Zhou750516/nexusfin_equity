import type { Locale } from "../locale";

const jointMessages: Record<Locale, Record<string, string>> = {
  "zh-CN": {
    "jointEntry.title": "联合登录中",
    "jointEntry.loadingDescription": "正在为您校验登录状态并恢复业务场景，请稍候。",
    "jointEntry.missingParams": "缺少联合登录参数，请返回来源页面重新进入。",
    "jointEntry.sessionExpired": "联合登录会话已失效，请返回来源页面重新进入。",
    "jointEntry.systemBusy": "联合登录暂时不可用，请稍后重试。",
    "jointEntry.unsupportedScene": "当前业务场景暂不支持，请稍后回到来源页面重试。",
    "jointDispatch.title": "分发页占位",
    "jointDispatch.subtitle": "联合登录已完成，当前进入分发页占位态。",
    "jointDispatch.orderLabel": "权益订单号",
    "jointDispatch.orderMissing": "未获取到权益订单号",
    "jointDispatch.nextStepsTitle": "后续说明",
    "jointRefund.title": "退款入口占位",
    "jointRefund.subtitle": "联合登录已完成，当前进入退款入口占位态。",
    "jointRefund.orderLabel": "权益订单号",
    "jointRefund.orderMissing": "未获取到权益订单号",
    "jointRefund.notesTitle": "当前说明"
  },
  "zh-TW": {
    "jointEntry.title": "聯合登入中",
    "jointEntry.loadingDescription": "正在為您校驗登入狀態並恢復業務場景，請稍候。",
    "jointEntry.missingParams": "缺少聯合登入參數，請返回來源頁面重新進入。",
    "jointEntry.sessionExpired": "聯合登入會話已失效，請返回來源頁面重新進入。",
    "jointEntry.systemBusy": "聯合登入暫時不可用，請稍後重試。",
    "jointEntry.unsupportedScene": "目前業務場景暫不支援，請稍後返回來源頁面重試。",
    "jointDispatch.title": "分發頁佔位",
    "jointDispatch.subtitle": "聯合登入已完成，目前進入分發頁佔位狀態。",
    "jointDispatch.orderLabel": "權益訂單號",
    "jointDispatch.orderMissing": "未取得權益訂單號",
    "jointDispatch.nextStepsTitle": "後續說明",
    "jointRefund.title": "退款入口佔位",
    "jointRefund.subtitle": "聯合登入已完成，目前進入退款入口佔位狀態。",
    "jointRefund.orderLabel": "權益訂單號",
    "jointRefund.orderMissing": "未取得權益訂單號",
    "jointRefund.notesTitle": "目前說明"
  },
  "en-US": {
    "jointEntry.title": "Joint Login",
    "jointEntry.loadingDescription": "We are validating your login session and restoring the business scene.",
    "jointEntry.missingParams": "Missing joint-login parameters. Please return to the source page and try again.",
    "jointEntry.sessionExpired": "The joint-login session has expired. Please return to the source page and try again.",
    "jointEntry.systemBusy": "Joint login is temporarily unavailable. Please try again later.",
    "jointEntry.unsupportedScene": "This business scenario is not supported yet. Please return to the source page and try again later.",
    "jointDispatch.title": "Dispatch Placeholder",
    "jointDispatch.subtitle": "Joint login has completed and you are now in the dispatch placeholder state.",
    "jointDispatch.orderLabel": "Benefit Order No.",
    "jointDispatch.orderMissing": "Benefit order number is unavailable",
    "jointDispatch.nextStepsTitle": "What happens next",
    "jointRefund.title": "Refund Entry Placeholder",
    "jointRefund.subtitle": "Joint login has completed and you are now in the refund-entry placeholder state.",
    "jointRefund.orderLabel": "Benefit Order No.",
    "jointRefund.orderMissing": "Benefit order number is unavailable",
    "jointRefund.notesTitle": "Current notes"
  },
  "vi-VN": {
    "jointEntry.title": "Đăng nhập liên hợp",
    "jointEntry.loadingDescription": "Hệ thống đang xác thực phiên đăng nhập và khôi phục ngữ cảnh nghiệp vụ cho bạn.",
    "jointEntry.missingParams": "Thiếu tham số đăng nhập liên hợp. Vui lòng quay lại trang nguồn và thử lại.",
    "jointEntry.sessionExpired": "Phiên đăng nhập liên hợp đã hết hạn. Vui lòng quay lại trang nguồn và thử lại.",
    "jointEntry.systemBusy": "Đăng nhập liên hợp tạm thời chưa khả dụng. Vui lòng thử lại sau.",
    "jointEntry.unsupportedScene": "Ngữ cảnh nghiệp vụ hiện chưa được hỗ trợ. Vui lòng quay lại trang nguồn và thử lại sau.",
    "jointDispatch.title": "Trang phân phát tạm thời",
    "jointDispatch.subtitle": "Đăng nhập liên hợp đã hoàn tất và bạn đang ở trạng thái trang phân phát tạm thời.",
    "jointDispatch.orderLabel": "Mã đơn quyền lợi",
    "jointDispatch.orderMissing": "Chưa lấy được mã đơn quyền lợi",
    "jointDispatch.nextStepsTitle": "Bước tiếp theo",
    "jointRefund.title": "Trang tiếp nhận hoàn tiền tạm thời",
    "jointRefund.subtitle": "Đăng nhập liên hợp đã hoàn tất và bạn đang ở trạng thái trang tiếp nhận hoàn tiền tạm thời.",
    "jointRefund.orderLabel": "Mã đơn quyền lợi",
    "jointRefund.orderMissing": "Chưa lấy được mã đơn quyền lợi",
    "jointRefund.notesTitle": "Lưu ý hiện tại"
  }
};

export default jointMessages;
