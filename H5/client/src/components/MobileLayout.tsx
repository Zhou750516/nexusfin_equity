/**
 * MobileLayout - 移动端页面容器
 * 固定最大宽度440px，居中显示，模拟手机屏幕
 */
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { ReactNode } from "react";

interface MobileLayoutProps {
  children: ReactNode;
  className?: string;
}

export default function MobileLayout({ children, className = "" }: MobileLayoutProps) {
  return (
    <div className="min-h-screen bg-[#f0f2f5] flex justify-center">
      <div className={`relative w-full max-w-[440px] min-h-screen bg-white flex flex-col shadow-xl ${className}`}>
        <LanguageSwitcher />
        {children}
      </div>
    </div>
  );
}
