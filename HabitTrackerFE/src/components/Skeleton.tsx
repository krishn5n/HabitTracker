import type { ReactNode } from "react";
import "./Skeleton.css";
import "./SummaryRow.css";

interface SkeletonProps {
  width?: string;
  height?: string;
  radius?: "sm" | "md" | "lg" | "pill";
}

/** A single shimmering placeholder block. Compose several to mirror the layout that is loading. */
export function Skeleton({ width = "100%", height = "1rem", radius = "sm" }: SkeletonProps) {
  return <span className={`skeleton skeleton--${radius}`} style={{ width, height }} aria-hidden />;
}

interface SkeletonRegionProps {
  label: string;
  className?: string;
  children: ReactNode;
}

/** Wraps a skeleton layout so assistive tech hears one "loading" message instead of empty blocks. */
export function SkeletonRegion({ label, className, children }: SkeletonRegionProps) {
  return (
    <div className={className} role="status" aria-busy="true">
      <span className="visually-hidden">{label}</span>
      {children}
    </div>
  );
}

interface SkeletonFieldProps {
  labelWidth?: string;
}

/** Label + control placeholder, sized to match `.field` / `.control`. */
export function SkeletonField({ labelWidth = "35%" }: SkeletonFieldProps) {
  return (
    <div className="field">
      <Skeleton width={labelWidth} height="0.875rem" />
      <Skeleton height="var(--control-height)" radius="md" />
    </div>
  );
}

interface SkeletonStatsProps {
  count?: number;
}

/** Row of label/value stat pairs, as in the field panel. */
export function SkeletonStats({ count = 3 }: SkeletonStatsProps) {
  return (
    <div className="skeleton-stats">
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="skeleton-stack">
          <Skeleton width="4rem" height="0.75rem" />
          <Skeleton width="3rem" height="1.25rem" />
        </div>
      ))}
    </div>
  );
}

/** Placeholder for the bordered summary strip used on the dashboard and field detail page. */
export function SkeletonSummaryRow() {
  return (
    <div className="summary-row">
      {[0, 1, 2].map((i) => (
        <div key={i} className="summary-row__item">
          <Skeleton width="2.5rem" height="2rem" />
          <Skeleton width="4rem" height="0.75rem" />
        </div>
      ))}
    </div>
  );
}
