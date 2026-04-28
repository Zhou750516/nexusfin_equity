import {
  Drawer,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { ChevronRight, X } from "lucide-react";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorProtocolDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  protocolKeys: readonly string[];
  allProtocolsAgreed: boolean;
  viewedProtocolCount: number;
  onViewProtocol: (key: string) => void;
  t: Translate;
}

export default function CalculatorProtocolDrawer({
  open,
  onOpenChange,
  protocolKeys,
  allProtocolsAgreed,
  viewedProtocolCount,
  onViewProtocol,
  t,
}: CalculatorProtocolDrawerProps) {
  return (
    <Drawer open={open} onOpenChange={onOpenChange}>
      <DrawerContent className="mx-auto w-full max-w-[440px] rounded-t-[24px] border-none bg-white p-0">
        <DrawerHeader className="flex flex-row items-center justify-between space-y-0 border-b border-h5-border-soft px-5 py-4">
          <DrawerTitle className="text-[17px] font-semibold tracking-tight text-h5-text-primary">
            {t("calculator.protocolDrawerTitle")}
          </DrawerTitle>
          <button
            type="button"
            onClick={() => onOpenChange(false)}
            aria-label={t("common.back")}
            className="flex size-6 items-center justify-center"
          >
            <X className="size-5 text-h5-text-secondary" strokeWidth={2} />
          </button>
        </DrawerHeader>

        <div className="flex flex-col gap-4 px-5 pb-4 pt-4">
          <div className="rounded-[14px] border border-h5-border-soft bg-h5-surface px-4 py-4">
            <p className="text-sm font-semibold leading-[21px] tracking-tight text-h5-text-secondary">
              {t("calculator.protocolImportant")}
            </p>
            <p className="mt-2 text-xs leading-[19.5px] text-h5-text-secondary">
              {t("calculator.protocolImportantBody")}
            </p>
          </div>

          <div className="flex flex-col gap-3">
            {protocolKeys.map((key) => (
              <button
                key={key}
                type="button"
                onClick={() => onViewProtocol(key)}
                className="flex h-[45px] items-center justify-between rounded-[14px] bg-h5-surface px-4 text-left"
              >
                <span className="text-sm leading-[21px] tracking-tight text-h5-text-primary">{t(key)}</span>
                <ChevronRight className="size-4 text-h5-text-secondary" strokeWidth={2} />
              </button>
            ))}
          </div>
        </div>

        <DrawerFooter className="border-t border-h5-border-soft px-5 pb-5 pt-4">
          <button
            type="button"
            disabled={!allProtocolsAgreed}
            onClick={() => onOpenChange(false)}
            className={`h-12 w-full rounded-full text-base font-medium tracking-tight transition-colors ${
              allProtocolsAgreed
                ? "bg-gradient-to-r from-h5-brand to-h5-brand-strong text-white"
                : "cursor-not-allowed bg-[#e5e6eb] text-[#c9cdd4]"
            }`}
          >
            {t("calculator.protocolAgreeButton", { count: viewedProtocolCount })}
          </button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
