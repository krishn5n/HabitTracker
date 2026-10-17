import { useEffect, useRef, useState } from "react";
import "./StatTile.css";

interface StatTileProps {
  value: number;
  total: number;
  label: string;
}

export function StatTile({ value, total, label }: StatTileProps) {
  const [display, setDisplay] = useState(0);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduceMotion) {
      setDisplay(value);
      return;
    }
    const el = ref.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (!entries[0].isIntersecting) return;
        observer.disconnect();
        const start = performance.now();
        const duration = 420;
        function tick(now: number) {
          const progress = Math.min(1, (now - start) / duration);
          setDisplay(Math.round(progress * value));
          if (progress < 1) requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);
      },
      { threshold: 0.4 },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [value]);

  return (
    <div className="stat-tile habit-card habit-card--wide" ref={ref}>
      <span className="stat-tile__value" aria-live="polite">
        {display}
        <span className="stat-tile__total">/{total}</span>
      </span>
      <span className="stat-tile__label">{label}</span>
    </div>
  );
}
