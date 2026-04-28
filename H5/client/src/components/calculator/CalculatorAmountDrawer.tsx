import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import { formatCurrency } from "@/lib/format";
import type { Locale } from "@/i18n/locale";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorAmountDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  amountRangeLabel: string;
  drawerStep: number;
  amountRangeMin?: number;
  amountRangeMax?: number;
  draftAmount: string;
  drawerQuickActions: number[];
  locale: Locale;
  onDraftAmountChange: (value: string) => void;
  onConfirm: () => void;
  t: Translate;
}

export default function CalculatorAmountDrawer({
  open,
  onOpenChange,
  amountRangeLabel,
  drawerStep,
  amountRangeMin,
  amountRangeMax,
  draftAmount,
  drawerQuickActions,
  locale,
  onDraftAmountChange,
  onConfirm,
  t,
}: CalculatorAmountDrawerProps) {
  return (
    <Drawer open={open} onOpenChange={onOpenChange}>
      <DrawerContent className="mx-auto w-full max-w-[440px] rounded-t-[24px] border-none bg-white">
        <DrawerHeader className="px-5 pb-2 pt-5 text-left">
          <DrawerTitle className="text-[18px] text-h5-text-primary">{t("calculator.editAmount")}</DrawerTitle>
          <DrawerDescription className="text-sm text-h5-text-secondary">{amountRangeLabel}</DrawerDescription>
        </DrawerHeader>

        <div className="px-5 pb-2">
          <input
            type="number"
            inputMode="numeric"
            min={amountRangeMin}
            max={amountRangeMax}
            step={drawerStep}
            value={draftAmount}
            onChange={(event) => onDraftAmountChange(event.target.value)}
            className="h-14 w-full rounded-2xl border border-h5-border-soft px-4 text-[28px] font-bold text-h5-text-primary outline-none"
          />
          <div className="mt-4 grid grid-cols-3 gap-3">
            {drawerQuickActions.map((quickAmount) => (
              <button
                key={quickAmount}
                onClick={() => onDraftAmountChange(String(quickAmount))}
                className="h-11 rounded-xl border border-h5-border-soft text-sm font-medium text-h5-text-primary"
              >
                {formatCurrency(quickAmount, locale)}
              </button>
            ))}
          </div>
        </div>

        <DrawerFooter className="px-5 pb-6 pt-3">
          <button onClick={onConfirm} className="h-14 w-full rounded-full bg-gradient-to-r from-h5-brand to-h5-brand-strong text-[17px] font-semibold text-white">
            {t("calculator.submit")}
          </button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}
