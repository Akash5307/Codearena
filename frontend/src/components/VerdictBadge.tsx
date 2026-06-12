import type { Verdict } from "../lib/types";
import { verdictLabel } from "../lib/format";

export function VerdictBadge({
  verdict,
  passed,
  total,
}: {
  verdict: Verdict;
  passed?: number;
  total?: number;
}) {
  let cls = "text-verdict-pending";
  if (verdict === "AC") cls = "text-verdict-ac font-bold";
  else if (verdict === "PENDING") cls = "text-verdict-pending";
  else cls = "text-verdict-wa font-bold";

  const showProgress =
    verdict !== "AC" && verdict !== "PENDING" && total != null && total > 0;

  return (
    <span className={cls}>
      {verdictLabel(verdict)}
      {verdict === "PENDING" && (
        <span className="ml-1 inline-block animate-pulse">…</span>
      )}
      {showProgress && (
        <span className="text-gray-500 font-normal"> on test {(passed ?? 0) + 1}</span>
      )}
    </span>
  );
}
