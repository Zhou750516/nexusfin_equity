import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";

type Translate = (key: string, params?: Record<string, string | number>) => string;

interface CalculatorBindCardDialogProps {
  open: boolean;
  message: string;
  onOpenChange: (open: boolean) => void;
  onBack: () => void;
  t: Translate;
}

export default function CalculatorBindCardDialog({
  open,
  message,
  onOpenChange,
  onBack,
  t,
}: CalculatorBindCardDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        showCloseButton={false}
        className="rounded-[24px] border-none bg-white px-6 py-7 shadow-[0_18px_60px_rgba(15,23,42,0.18)]"
      >
        <CalculatorBindCardDialogContent
          message={message}
          onAck={() => onOpenChange(false)}
          onBack={onBack}
          t={t}
          useDialogPrimitives
        />
      </DialogContent>
    </Dialog>
  );
}

export function CalculatorBindCardDialogContent({
  message,
  onAck,
  onBack,
  t,
  useDialogPrimitives = false,
}: {
  message: string;
  onAck: () => void;
  onBack: () => void;
  t: Translate;
  useDialogPrimitives?: boolean;
}) {
  const title = t("calculator.bindCardTitle");
  return (
    <>
      {useDialogPrimitives ? (
        <>
          <DialogTitle className="text-center text-[18px] font-semibold leading-[27px] tracking-tight text-h5-text-primary">
            {title}
          </DialogTitle>
          <DialogDescription className="mt-3 text-center text-[15px] leading-[24px] tracking-tight text-h5-text-secondary">
            {message}
          </DialogDescription>
        </>
      ) : (
        <>
          <h2 className="text-center text-[18px] font-semibold leading-[27px] tracking-tight text-h5-text-primary">
            {title}
          </h2>
          <p className="mt-3 text-center text-[15px] leading-[24px] tracking-tight text-h5-text-secondary">
            {message}
          </p>
        </>
      )}
      <div className="mt-6 grid grid-cols-2 gap-3">
        <button
          type="button"
          className="h-11 rounded-full border border-h5-border-soft bg-white text-[15px] font-semibold text-h5-text-primary"
          onClick={onBack}
        >
          {t("calculator.bindCardBack")}
        </button>
        <button
          type="button"
          className="h-11 rounded-full bg-h5-brand text-[15px] font-semibold text-white"
          onClick={onAck}
        >
          {t("calculator.bindCardAck")}
        </button>
      </div>
    </>
  );
}
