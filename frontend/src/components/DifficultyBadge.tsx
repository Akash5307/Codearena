import { difficultyColorClass } from "../lib/format";

export function DifficultyBadge({ difficulty }: { difficulty: string | null }) {
  if (!difficulty) return <span className="text-gray-400">—</span>;
  return (
    <span className={`font-bold ${difficultyColorClass(difficulty)}`}>
      {difficulty.charAt(0) + difficulty.slice(1).toLowerCase()}
    </span>
  );
}
