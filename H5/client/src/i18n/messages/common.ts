import type { Locale } from "../locale";

const commonMessages: Record<Locale, Record<string, string>> = {
  "zh-CN": {
    "common.back": "返回",
    "common.retry": "重试",
    "common.completed": "已完成",
    "common.inProgress": "进行中",
    "common.pending": "待审批",
    "common.ad": "广告",
    "language.label": "语言"
  },
  "zh-TW": {
    "common.back": "返回",
    "common.retry": "重試",
    "common.completed": "已完成",
    "common.inProgress": "進行中",
    "common.pending": "待審批",
    "common.ad": "廣告",
    "language.label": "語言"
  },
  "en-US": {
    "common.back": "Back",
    "common.retry": "Retry",
    "common.completed": "Completed",
    "common.inProgress": "In Progress",
    "common.pending": "Pending",
    "common.ad": "Ad",
    "language.label": "Language"
  },
  "vi-VN": {
    "common.back": "Quay lại",
    "common.retry": "Thử lại",
    "common.completed": "Hoàn tất",
    "common.inProgress": "Đang xử lý",
    "common.pending": "Chờ duyệt",
    "common.ad": "Quảng cáo",
    "language.label": "Ngôn ngữ"
  }
};

export default commonMessages;
