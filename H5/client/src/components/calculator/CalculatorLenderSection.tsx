import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import SectionCard from "@/components/shared/SectionCard";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface Partner {
  short: string;
  full: string;
  color: string;
}

interface CalculatorLenderSectionProps {
  annualRateValue: string;
  partners: Partner[];
  partnersDialogOpen: boolean;
  onOpenChange: (open: boolean) => void;
  t: Translate;
}

export default function CalculatorLenderSection({
  annualRateValue,
  partners,
  partnersDialogOpen,
  onOpenChange,
  t,
}: CalculatorLenderSectionProps) {
  return (
    <>
      <SectionCard className="py-5">
        <div className="flex items-center justify-between">
          <span className="text-[15px] tracking-tight text-h5-text-secondary">{t("calculator.annualRate")}</span>
          <div className="text-right">
            <p className="text-sm font-medium leading-[21px] tracking-tight text-h5-text-secondary">
              {annualRateValue}
              {t("calculator.annualRateMethod")}
            </p>
            <p className="mt-1 text-xs leading-[18px] text-h5-text-secondary">
              {t("calculator.annualRateTip")}
            </p>
          </div>
        </div>
      </SectionCard>

      <Dialog open={partnersDialogOpen} onOpenChange={onOpenChange}>
        <DialogContent
          showCloseButton={false}
          className="w-[calc(100%-44px)] max-w-[400px] gap-0 rounded-3xl border-none bg-white px-6 py-6 sm:max-w-none"
        >
          <DialogTitle className="text-center text-[18px] font-semibold leading-[27px] tracking-tight text-h5-text-primary">
            {t("calculator.partnersTitle")}
          </DialogTitle>
          <DialogDescription className="mt-3 text-center text-sm leading-[22.75px] tracking-tight text-h5-text-secondary">
            {t("calculator.partnersDescription")}
          </DialogDescription>

          <div className="mt-5 grid grid-cols-2 gap-x-4 gap-y-4">
            {partners.map((partner) => (
              <div key={partner.full} className="flex items-center gap-3">
                <div
                  className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-xs leading-[18px] text-white"
                  style={{ backgroundColor: partner.color }}
                >
                  {partner.short}
                </div>
                <span className="text-sm leading-[21px] tracking-tight text-h5-text-primary">{partner.full}</span>
              </div>
            ))}
          </div>

          <p className="mt-4 text-center text-[13px] leading-[19.5px] tracking-tight text-h5-text-secondary">
            {t("calculator.partnersFootnote")}
          </p>

          <button
            type="button"
            onClick={() => onOpenChange(false)}
            className="mt-5 h-12 w-full rounded-full bg-[#fbaf19] text-base font-medium leading-6 tracking-tight text-white"
          >
            {t("calculator.partnersAck")}
          </button>
        </DialogContent>
      </Dialog>
    </>
  );
}
