import { useEffect, useState } from "react";
import { userApi } from "../lib/services";
import type { Page, UserProfile } from "../lib/types";
import { ApiError } from "../lib/api";
import { RatingName } from "../components/RatingName";
import { Pagination } from "../components/Pagination";
import { Spinner, ErrorBox } from "../components/Spinner";

const PAGE_SIZE = 50;

export function Ratings() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<UserProfile> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    userApi
      .ratings({ page, size: PAGE_SIZE, sort: "rating,desc" })
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load leaderboard"))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div className="cf-panel">
      <div className="cf-panel-title">Rating leaderboard</div>
      <div className="p-2">
        {loading ? (
          <Spinner />
        ) : error ? (
          <ErrorBox message={error} />
        ) : !data || data.content.length === 0 ? (
          <p className="p-4 text-gray-500">No users yet.</p>
        ) : (
          <>
            <table className="cf-table">
              <thead>
                <tr>
                  <th className="w-16">#</th>
                  <th>Who</th>
                  <th className="w-28 text-right">Rating</th>
                  <th className="w-28 text-right">Max</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((u, i) => (
                  <tr key={u.id}>
                    <td className="text-gray-500">{page * PAGE_SIZE + i + 1}</td>
                    <td>
                      <RatingName username={u.username} rating={u.rating} />
                    </td>
                    <td className="text-right font-bold">{u.rating}</td>
                    <td className="text-right text-gray-600">{u.maxRating}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
          </>
        )}
      </div>
    </div>
  );
}
