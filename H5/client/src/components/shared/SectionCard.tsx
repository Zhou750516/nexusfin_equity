import { cn } from "@/lib/utils";
import type { PropsWithChildren, ReactNode } from "react";

type SectionCardProps = PropsWithChildren<{
  title?: ReactNode;
  subtitle?: ReactNode;
  action?: ReactNode;
  className?: string;
  contentClassName?: string;
}>;

export default function SectionCard({
  title,
  subtitle,
  action,
  className,
  contentClassName,
  children,
}: SectionCardProps) {
  return (
    <section className={cn("h5-card", className)}>
      {title || subtitle || action ? (
        <header className="flex items-start justify-between gap-3">
          <div className="min-w-0 flex-1">
            {title ? <h3 className="h5-card-title">{title}</h3> : null}
            {subtitle ? <p className="mt-1 h5-card-subtitle">{subtitle}</p> : null}
          </div>
          {action}
        </header>
      ) : null}
      <div className={cn(title || subtitle || action ? "mt-4" : "", contentClassName)}>{children}</div>
    </section>
  );
}
