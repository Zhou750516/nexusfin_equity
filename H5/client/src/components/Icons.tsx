/**
 * 内联SVG图标组件 - 来自Figma设计稿
 */

// 时钟图标（白色，用于蓝色背景）
export const ClockIcon = ({ className = "w-9 h-9" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M20 36.6666C29.2048 36.6666 36.6667 29.2047 36.6667 20C36.6667 10.7952 29.2048 3.33333 20 3.33333C10.7953 3.33333 3.33337 10.7952 3.33337 20C3.33337 29.2047 10.7953 36.6666 20 36.6666Z" stroke="white" strokeWidth="3.33321" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M20 10V20L26.6667 23.3333" stroke="white" strokeWidth="3.33321" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 勾选圆圈图标（白色，用于蓝色背景）
export const CheckCircleIcon = ({ className = "w-5 h-5" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12.0004 22.0008C17.5235 22.0008 22.0008 17.5235 22.0008 12.0004C22.0008 6.47739 17.5235 2.00007 12.0004 2.00007C6.47739 2.00007 2.00007 6.47739 2.00007 12.0004C2.00007 17.5235 6.47739 22.0008 12.0004 22.0008Z" stroke="white" strokeWidth="1.99993" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M9.00003 12.0001L11.0001 14.0001L15.0003 10" stroke="white" strokeWidth="1.99993" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 小时钟图标（白色，用于蓝色背景）
export const ClockSmallIcon = ({ className = "w-5 h-5" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12.0004 22.0008C17.5235 22.0008 22.0008 17.5235 22.0008 12.0004C22.0008 6.47739 17.5235 2.00007 12.0004 2.00007C6.47739 2.00007 2.00007 6.47739 2.00007 12.0004C2.00007 17.5235 6.47739 22.0008 12.0004 22.0008Z" stroke="white" strokeWidth="1.99993" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M12 6V12.0002L16.0002 14.0003" stroke="white" strokeWidth="1.99993" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 文档图标（灰色，用于灰色背景）
export const DocIcon = ({ className = "w-5 h-5", color = "#86909C" }: { className?: string; color?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M15.0006 2.00007H6.00022C5.46977 2.00007 4.96104 2.2108 4.58596 2.58588C4.21087 2.96097 4.00015 3.46969 4.00015 4.00015V20.0007C4.00015 20.5312 4.21087 21.0399 4.58596 21.415C4.96104 21.7901 5.46977 22.0008 6.00022 22.0008H18.0007C18.5311 22.0008 19.0398 21.7901 19.4149 21.415C19.79 21.0399 20.0007 20.5312 20.0007 20.0007V7.00026L15.0006 2.00007Z" stroke={color} strokeWidth="1.99993" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M14 2V6.00014C14 6.5306 14.2107 7.03932 14.5858 7.41441C14.9609 7.7895 15.4696 8.00022 16 8.00022H20.0002" stroke={color} strokeWidth="1.99993" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 卡片图标（白色，用于橙色背景）
export const CardIcon = ({ className = "w-4 h-4" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 14 11" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12.375 0H1.375C0.611875 0 0.00687499 0.611875 0.00687499 1.375L0 9.625C0 10.3881 0.611875 11 1.375 11H12.375C13.1381 11 13.75 10.3881 13.75 9.625V1.375C13.75 0.611875 13.1381 0 12.375 0ZM12.375 9.625H1.375V5.5H12.375V9.625ZM12.375 2.75H1.375V1.375H12.375V2.75Z" fill="white"/>
  </svg>
);

// 右箭头图标（橙色）
export const ChevronRightOrange = ({ className = "w-3.5 h-3.5" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 5 9" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M0.624951 8.12518L4.37503 4.3751L0.624951 0.625016" stroke="#FF6B00" strokeWidth="1.16613" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 蓝色勾选图标（蓝色）
export const CheckBlueIcon = ({ className = "w-3 h-3" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 9 7" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M0.681843 3.95454L2.86366 6.13636L8.31821 0.68181" stroke="#165DFF" strokeWidth="1.24995" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 审批结果页图标
export const CheckCircleBlue = ({ className = "w-9 h-9" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="20" cy="20" r="18" stroke="white" strokeWidth="3"/>
    <path d="M12 20L17 25L28 14" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 信息/感叹号图标（白色）
export const InfoIcon = ({ className = "w-5 h-5" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="12" cy="12" r="10" stroke="white" strokeWidth="2"/>
    <path d="M12 8V12" stroke="white" strokeWidth="2" strokeLinecap="round"/>
    <circle cx="12" cy="16" r="1" fill="white"/>
  </svg>
);

// 橙色感叹号图标（用于温馨提示）
export const InfoOrangeIcon = ({ className = "w-5 h-5" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="12" cy="12" r="10" stroke="#FF9500" strokeWidth="2"/>
    <path d="M12 8V12" stroke="#FF9500" strokeWidth="2" strokeLinecap="round"/>
    <circle cx="12" cy="16" r="1" fill="#FF9500"/>
  </svg>
);

// 返回箭头图标
export const BackArrow = ({ className = "w-6 h-6", color = "#1d2129" }: { className?: string; color?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M15 18L9 12L15 6" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 银行卡图标（蓝色）
export const BankCardIcon = ({ className = "w-5 h-5" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="2" y="5" width="20" height="14" rx="2" stroke="#165DFF" strokeWidth="1.5"/>
    <path d="M2 10H22" stroke="#165DFF" strokeWidth="1.5"/>
    <rect x="5" y="13" width="4" height="2" rx="1" fill="#165DFF"/>
  </svg>
);

// 右箭头（灰色）
export const ChevronRightGray = ({ className = "w-4 h-4" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M9 18L15 12L9 6" stroke="#86909C" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 还款成功图标（白色）
export const SuccessIcon = ({ className = "w-9 h-9" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    <circle cx="20" cy="20" r="18" stroke="white" strokeWidth="3"/>
    <path d="M12 20L17 25L28 14" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 日历图标（蓝色）
export const CalendarIcon = ({ className = "w-5 h-5" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="3" y="4" width="18" height="18" rx="2" stroke="#165DFF" strokeWidth="1.5"/>
    <path d="M3 9H21" stroke="#165DFF" strokeWidth="1.5"/>
    <path d="M8 2V6M16 2V6" stroke="#165DFF" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
);

// 节省利息图标（绿色下降箭头）
export const SavingIcon = ({ className = "w-5 h-5" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M3 7L9 13L13 9L21 17" stroke="#22C55E" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M17 17H21V13" stroke="#22C55E" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 影音图标（蓝色）
export const TvIcon = ({ className = "w-6 h-6", active = false }: { className?: string; active?: boolean }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="2" y="3" width="20" height="15" rx="2" stroke={active ? "#165DFF" : "#86909C"} strokeWidth="1.5"/>
    <path d="M8 21H16M12 18V21" stroke={active ? "#165DFF" : "#86909C"} strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
);

// 出行图标（蓝色）
export const CarIcon = ({ className = "w-6 h-6", active = false }: { className?: string; active?: boolean }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M5 17H3V11L6 5H18L21 11V17H19M5 17C5 18.1 5.9 19 7 19C8.1 19 9 18.1 9 17M5 17C5 15.9 5.9 15 7 15C8.1 15 9 15.9 9 17M19 17C19 18.1 18.1 19 17 19C15.9 19 15 18.1 15 17M19 17C19 15.9 18.1 15 17 15C15.9 15 15 15.9 15 17M9 17H15" stroke={active ? "#165DFF" : "#86909C"} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 生活图标
export const LifeIcon = ({ className = "w-6 h-6", active = false }: { className?: string; active?: boolean }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M3 9L12 2L21 9V20C21 20.5523 20.5523 21 20 21H4C3.44772 21 3 20.5523 3 20V9Z" stroke={active ? "#165DFF" : "#86909C"} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M9 21V12H15V21" stroke={active ? "#165DFF" : "#86909C"} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// 购物图标
export const ShopIcon = ({ className = "w-6 h-6", active = false }: { className?: string; active?: boolean }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M6 2L3 6V20C3 20.5523 3.44772 21 4 21H20C20.5523 21 21 20.5523 21 20V6L18 2H6Z" stroke={active ? "#165DFF" : "#86909C"} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M3 6H21" stroke={active ? "#165DFF" : "#86909C"} strokeWidth="1.5" strokeLinecap="round"/>
    <path d="M16 10C16 12.2091 14.2091 14 12 14C9.79086 14 8 12.2091 8 10" stroke={active ? "#165DFF" : "#86909C"} strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
);
