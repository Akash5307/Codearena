import { Link } from "react-router-dom";
import { ratingColorClass } from "../lib/format";

// Codeforces colors usernames by rating. We don't always have the rating in
// list responses, so rating is optional (defaults to a neutral color).
export function RatingName({
  username,
  rating,
  link = true,
}: {
  username: string;
  rating?: number;
  link?: boolean;
}) {
  const cls = `font-bold ${rating != null ? ratingColorClass(rating) : "text-cf-blue"}`;
  if (!link) return <span className={cls}>{username}</span>;
  return (
    <Link to={`/users/${username}`} className={`${cls} hover:underline`}>
      {username}
    </Link>
  );
}
