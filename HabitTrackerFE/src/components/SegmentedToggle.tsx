import type { CSSProperties } from "react";
import "./SegmentedToggle.css";

interface SegmentedToggleProps<T extends string> {
  options: { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
}

export function SegmentedToggle<T extends string>({ options, value, onChange }: SegmentedToggleProps<T>) {
  const index = options.findIndex((o) => o.value === value);
  const widthPct = 100 / options.length;

  return (
    <div
      className="segmented-toggle"
      role="tablist"
      style={
        {
          "--segmented-toggle-thumb-width": `${widthPct}%`,
          "--segmented-toggle-thumb-x": `${index * 100}%`,
        } as CSSProperties
      }
    >
      <span className="segmented-toggle__thumb" aria-hidden />
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          role="tab"
          aria-selected={option.value === value}
          className={`segmented-toggle__option${option.value === value ? " segmented-toggle__option--active" : ""}`}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
