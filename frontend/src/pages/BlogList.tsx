import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { blogApi } from "../lib/services";
import type { BlogListItem, Page } from "../lib/types";
import { ApiError } from "../lib/api";
import { useAuth } from "../store/auth";
import { RatingName } from "../components/RatingName";
import { Pagination } from "../components/Pagination";
import { Spinner, ErrorBox } from "../components/Spinner";
import { formatDateTime } from "../lib/format";

const PAGE_SIZE = 20;

export function BlogList() {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const page = Number(params.get("page") ?? "0");

  const [data, setData] = useState<Page<BlogListItem> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    blogApi
      .list({ page, size: PAGE_SIZE, sort: "createdAt,desc" })
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load posts"))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div className="cf-panel">
      <div className="cf-panel-title flex items-center justify-between">
        <span>Blog</span>
        {user && (
          <Link
            to="/blogs/new"
            className="rounded-sm bg-white/80 px-2 py-0.5 text-[12px] font-bold text-cf-blue no-underline hover:bg-white"
          >
            + New post
          </Link>
        )}
      </div>
      <div className="p-2">
        {loading ? (
          <Spinner />
        ) : error ? (
          <ErrorBox message={error} />
        ) : !data || data.content.length === 0 ? (
          <p className="p-4 text-gray-500">No posts yet.</p>
        ) : (
          <>
            <table className="cf-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th className="w-28">Author</th>
                  <th className="w-16 text-center">Score</th>
                  <th className="w-20 text-center">Comments</th>
                  <th className="w-40">Posted</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <Link to={`/blogs/${p.id}`} className="font-bold">
                        {p.title}
                      </Link>
                    </td>
                    <td>
                      <RatingName username={p.authorUsername} />
                    </td>
                    <td className="text-center">
                      <span className={p.upvotes - p.downvotes >= 0 ? "text-verdict-ac" : "text-verdict-wa"}>
                        {p.upvotes - p.downvotes >= 0 ? "+" : ""}
                        {p.upvotes - p.downvotes}
                      </span>
                    </td>
                    <td className="text-center text-gray-600">{p.commentCount}</td>
                    <td className="text-[12px] text-gray-600">{formatDateTime(p.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination
              page={data.number}
              totalPages={data.totalPages}
              onChange={(p) => {
                const merged = new URLSearchParams(params);
                merged.set("page", String(p));
                setParams(merged);
              }}
            />
          </>
        )}
      </div>
    </div>
  );
}
