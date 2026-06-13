import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import rehypeKatex from "rehype-katex";
import { blogApi } from "../lib/services";
import type { BlogComment, BlogDetail as Post } from "../lib/types";
import { ApiError } from "../lib/api";
import { useAuth } from "../store/auth";
import { RatingName } from "../components/RatingName";
import { Spinner, ErrorBox } from "../components/Spinner";
import { formatDateTime } from "../lib/format";

export function BlogDetail() {
  const { id = "" } = useParams();
  const postId = Number(id);
  const { user } = useAuth();
  const navigate = useNavigate();

  const [post, setPost] = useState<Post | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [voting, setVoting] = useState(false);

  const reload = useCallback(() => {
    return blogApi
      .get(postId)
      .then(setPost)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load post"))
      .finally(() => setLoading(false));
  }, [postId]);

  useEffect(() => {
    setLoading(true);
    setError(null);
    reload();
  }, [reload]);

  async function vote(voteType: "UPVOTE" | "DOWNVOTE") {
    if (!user || !post) return;
    setVoting(true);
    try {
      await blogApi.vote(post.id, voteType);
      await reload();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Vote failed");
    } finally {
      setVoting(false);
    }
  }

  if (loading) return <Spinner />;
  if (error) return <ErrorBox message={error} />;
  if (!post) return null;

  const isAuthor = user?.username === post.authorUsername;

  return (
    <div className="mx-auto max-w-[820px] space-y-4">
      <article className="cf-panel p-5">
        <div className="flex items-start gap-3">
          {/* Vote rail */}
          <div className="flex flex-col items-center pt-1 text-[13px]">
            <button
              className="leading-none hover:text-verdict-ac disabled:opacity-40"
              onClick={() => vote("UPVOTE")}
              disabled={!user || voting}
              title={user ? "Upvote" : "Log in to vote"}
            >
              ▲
            </button>
            <span className="font-bold">{post.upvotes - post.downvotes}</span>
            <button
              className="leading-none hover:text-verdict-wa disabled:opacity-40"
              onClick={() => vote("DOWNVOTE")}
              disabled={!user || voting}
              title={user ? "Downvote" : "Log in to vote"}
            >
              ▼
            </button>
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-3">
              <h1 className="text-[22px] font-bold">{post.title}</h1>
              {isAuthor && (
                <button
                  className="cf-btn shrink-0"
                  onClick={() => navigate(`/blogs/${post.id}/edit`)}
                >
                  Edit
                </button>
              )}
            </div>
            <div className="mt-1 text-[12px] text-gray-500">
              by <RatingName username={post.authorUsername} /> · {formatDateTime(post.createdAt)}
              {post.updatedAt !== post.createdAt && " · edited"}
              <span className="ml-2 text-verdict-ac">+{post.upvotes}</span>
              <span className="ml-1 text-verdict-wa">−{post.downvotes}</span>
            </div>

            <div className="statement mt-4">
              <ReactMarkdown
                remarkPlugins={[remarkGfm, remarkMath]}
                rehypePlugins={[rehypeKatex]}
              >
                {post.content}
              </ReactMarkdown>
            </div>
          </div>
        </div>
      </article>

      <Comments post={post} onChanged={reload} />
    </div>
  );
}

function Comments({ post, onChanged }: { post: Post; onChanged: () => Promise<void> }) {
  const { user } = useAuth();
  const count = countComments(post.comments);
  return (
    <div className="cf-panel">
      <div className="cf-panel-title">Comments ({count})</div>
      <div className="space-y-3 p-3">
        {user ? (
          <CommentForm postId={post.id} parentId={null} onDone={onChanged} placeholder="Write a comment…" />
        ) : (
          <p className="text-[13px] text-gray-600">
            <Link to="/login" className="font-bold">Enter</Link> to comment.
          </p>
        )}
        {post.comments.length === 0 ? (
          <p className="text-[13px] text-gray-500">No comments yet.</p>
        ) : (
          post.comments.map((c) => (
            <CommentNode key={c.id} comment={c} postId={post.id} onChanged={onChanged} />
          ))
        )}
      </div>
    </div>
  );
}

function CommentNode({
  comment,
  postId,
  onChanged,
}: {
  comment: BlogComment;
  postId: number;
  onChanged: () => Promise<void>;
}) {
  const { user } = useAuth();
  const [replying, setReplying] = useState(false);

  return (
    <div className="border-l-2 border-cf-border pl-3">
      <div className="text-[12px] text-gray-500">
        <RatingName username={comment.authorUsername} /> · {formatDateTime(comment.createdAt)}
      </div>
      <div className="whitespace-pre-wrap text-[13px]">{comment.content}</div>
      {user && (
        <button
          className="mt-0.5 text-[11px] text-cf-blue hover:underline"
          onClick={() => setReplying((r) => !r)}
        >
          {replying ? "cancel" : "reply"}
        </button>
      )}
      {replying && (
        <div className="mt-2">
          <CommentForm
            postId={postId}
            parentId={comment.id}
            onDone={async () => {
              setReplying(false);
              await onChanged();
            }}
            placeholder={`Reply to ${comment.authorUsername}…`}
          />
        </div>
      )}
      {comment.replies.length > 0 && (
        <div className="mt-2 space-y-2">
          {comment.replies.map((r) => (
            <CommentNode key={r.id} comment={r} postId={postId} onChanged={onChanged} />
          ))}
        </div>
      )}
    </div>
  );
}

function CommentForm({
  postId,
  parentId,
  onDone,
  placeholder,
}: {
  postId: number;
  parentId: number | null;
  onDone: () => void | Promise<void>;
  placeholder: string;
}) {
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!text.trim()) return;
    setBusy(true);
    setErr(null);
    try {
      await blogApi.addComment(postId, text.trim(), parentId);
      setText("");
      await onDone();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Failed to comment");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-1">
      {err && <ErrorBox message={err} />}
      <textarea
        className="cf-input text-[13px]"
        rows={2}
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={placeholder}
      />
      <button type="submit" className="cf-btn cf-btn-primary" disabled={busy}>
        {busy ? "Posting…" : "Post"}
      </button>
    </form>
  );
}

function countComments(comments: BlogComment[]): number {
  return comments.reduce((n, c) => n + 1 + countComments(c.replies), 0);
}
