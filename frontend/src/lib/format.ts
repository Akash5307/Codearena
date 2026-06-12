import type { Role, Verdict } from "./types";

export function canSetProblems(role: Role | undefined): boolean {
  return role === "ADMIN" || role === "PROBLEM_SETTER";
}

export function isAdmin(role: Role | undefined): boolean {
  return role === "ADMIN";
}

export function ratingColorClass(rating: number): string {
  if (rating < 1200) return "text-rating-newbie";
  if (rating < 1400) return "text-rating-pupil";
  if (rating < 1600) return "text-rating-specialist";
  if (rating < 1900) return "text-rating-expert";
  if (rating < 2100) return "text-rating-candidate";
  if (rating < 2400) return "text-rating-master";
  return "text-rating-grandmaster";
}

export function difficultyColorClass(difficulty: string | null): string {
  switch ((difficulty ?? "").toUpperCase()) {
    case "EASY":
      return "text-rating-pupil";
    case "MEDIUM":
      return "text-rating-master";
    case "HARD":
      return "text-rating-grandmaster";
    default:
      return "text-gray-600";
  }
}

export const VERDICT_LABELS: Record<string, string> = {
  PENDING: "In queue",
  AC: "Accepted",
  WA: "Wrong answer",
  TLE: "Time limit exceeded",
  MLE: "Memory limit exceeded",
  RE: "Runtime error",
  CE: "Compilation error",
  JUDGING: "Judging",
};

export function verdictLabel(verdict: Verdict): string {
  return VERDICT_LABELS[verdict] ?? verdict;
}

export function isPending(verdict: Verdict): boolean {
  return verdict === "PENDING";
}

export function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatMs(ms: number | null): string {
  if (ms == null) return "—";
  return `${ms} ms`;
}

export function formatKb(kb: number | null): string {
  if (kb == null) return "—";
  if (kb >= 1024) return `${(kb / 1024).toFixed(1)} MB`;
  return `${kb} KB`;
}

export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h && m) return `${h}h ${m}m`;
  if (h) return `${h}h`;
  return `${m}m`;
}

export const CONTEST_STATE_LABELS: Record<string, string> = {
  BEFORE: "Upcoming",
  RUNNING: "Running",
  ENDED: "Finished",
};

export function contestStateClass(state: string): string {
  switch (state) {
    case "RUNNING":
      return "text-verdict-ac font-bold";
    case "BEFORE":
      return "text-cf-blue font-bold";
    default:
      return "text-gray-500";
  }
}

// "2d 3h", "4h 12m", "8m 30s", or "now" — for countdowns to start/end.
export function formatCountdown(targetIso: string): string {
  const diff = new Date(targetIso).getTime() - Date.now();
  if (diff <= 0) return "now";
  const s = Math.floor(diff / 1000);
  const d = Math.floor(s / 86400);
  const h = Math.floor((s % 86400) / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  if (d) return `${d}d ${h}h`;
  if (h) return `${h}h ${m}m`;
  if (m) return `${m}m ${sec}s`;
  return `${sec}s`;
}
