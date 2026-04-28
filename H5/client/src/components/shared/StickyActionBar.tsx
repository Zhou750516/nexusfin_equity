import { cn } from "@/lib/utils";
import type { PropsWithChildren, ReactNode } from "react";

type StickyActionBarProps = PropsWithChildren<{
  primary?: ReactNode;
  secondary?: ReactNode;
  className?: string;
}>;

export default function StickyActionBar({
  primary,
  secondary,
  className,
  children,
}: StickyActionBarProps) {
  return (
    <div className={cn("h5-sticky-bar", className)}>
      {children}
      {secondary}
      {primary}
    </div>
  );
}
