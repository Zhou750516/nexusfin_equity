import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { Check, X } from "lucide-react";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorPurposeDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  purposeKey: string;
  loanPurposeKeys: readonly string[];
  onSelect: (key: string) => void;
  t: Translate;
}

export default function CalculatorPurposeDrawer({
  open,
  onOpenChange,
  purposeKey,
  loanPurposeKeys,
  onSelect,
  t,
}: CalculatorPurposeDrawerProps) {
  return (
    <Drawer open={open} onOpenChange={onOpenChange}>
      <DrawerContent className="mx-auto w-full max-w-[440px] rounded-t-[24px] border-none bg-white">
        <DrawerHeader className="flex flex-row items-center justify-between space-y-0 border-b border-h5-border-soft px-5 py-4">
          <DrawerTitle className="text-[17px] font-semibold tracking-tight text-h5-text-primary">
            {t("calculator.loanPurposeTitle")}
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
        <div className="flex flex-col px-5 pb-6 pt-2">
          {loanPurposeKeys.map((key) => {
            const selected = key === purposeKey;
            return (
              <button
                key={key}
                type="button"
                onClick={() => onSelect(key)}
                className={`flex h-[54px] items-center justify-between rounded-[14px] px-4 transition-colors ${
                  selected ? "bg-h5-brand/8" : "bg-transparent"
                }`}
              >
                <span
                  className={`text-[15px] font-medium tracking-tight ${
                    selected ? "text-h5-brand" : "text-h5-text-primary"
                  }`}
                >
                  {t(key)}
                </span>
                {selected ? <Check className="size-5 text-h5-brand" strokeWidth={2} /> : null}
              </button>
            );
          })}
        </div>
      </DrawerContent>
    </Drawer>
  );
}
