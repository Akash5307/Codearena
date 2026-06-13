import { api } from "./api";
import type {
  BlogComment,
  BlogDetail,
  BlogListItem,
  ContestDetail,
  ContestListItem,
  Page,
  ProblemDetail,
  ProblemListItem,
  SampleTestCaseContent,
  Standings,
  SubmissionDetail,
  SubmissionListItem,
  SubmitRequest,
  Tag,
  TokenResponse,
  UserProfile,
} from "./types";

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
}

// ---- Auth ----
export const authApi = {
  register: (body: { username: string; email: string; password: string }) =>
    api.post<TokenResponse>("/auth/register", body),
  login: (body: { usernameOrEmail: string; password: string }) =>
    api.post<TokenResponse>("/auth/login", body),
  logout: (refreshToken: string | null) =>
    api.post<string>("/auth/logout", { refreshToken }),
};

// ---- Problems ----
export interface ProblemCreateBody {
  title: string;
  statement: string;
  inputFormat?: string;
  outputFormat?: string;
  difficulty: string;
  timeLimitMs: number;
  memoryLimitMb: number;
  tags: string[];
}

export const problemApi = {
  list: (params: PageParams & { title?: string; difficulty?: string; tag?: string }) =>
    api.get<Page<ProblemListItem>>("/problems", { ...params }),
  get: (slug: string) => api.get<ProblemDetail>(`/problems/${slug}`),
  samples: (slug: string) =>
    api.get<SampleTestCaseContent[]>(`/problems/${slug}/samples`),
  tags: () => api.get<Tag[]>("/problems/tags"),
  create: (body: ProblemCreateBody) =>
    api.post<ProblemDetail>("/problems", body),
  update: (id: number, body: Partial<ProblemCreateBody> & { isPublished?: boolean }) =>
    api.put<ProblemDetail>(`/problems/${id}`, body),
  uploadTestCase: (id: number, input: File, output: File, sample: boolean) => {
    const form = new FormData();
    form.append("input", input);
    form.append("output", output);
    form.append("sample", String(sample));
    return api.upload<unknown>(`/problems/${id}/test-cases`, form);
  },
};

// ---- Submissions ----
export const submissionApi = {
  submit: (body: SubmitRequest) =>
    api.post<SubmissionDetail>("/submissions", body),
  get: (id: number) => api.get<SubmissionDetail>(`/submissions/${id}`),
  list: (
    params: PageParams & {
      userId?: number;
      problemId?: number;
      language?: string;
      verdict?: string;
    },
  ) => api.get<Page<SubmissionListItem>>("/submissions", { ...params }),
};

// ---- Contests ----
export interface ContestCreateBody {
  title: string;
  description?: string;
  type: string;
  startTime: string; // ISO-8601 local datetime
  durationMinutes: number;
  isRated: boolean;
  problems: { problemId: number; label: string; orderIndex: number; points: number | null }[];
}

export const contestApi = {
  list: (params: PageParams & { status?: string }) =>
    api.get<Page<ContestListItem>>("/contests", { ...params }),
  get: (slug: string) => api.get<ContestDetail>(`/contests/${slug}`),
  create: (body: ContestCreateBody) =>
    api.post<ContestDetail>("/contests", body),
  register: (id: number) => api.post<string>(`/contests/${id}/register`, {}),
  standings: (id: number) => api.get<Standings>(`/contests/${id}/standings`),
  mySubmissions: (id: number, params: PageParams) =>
    api.get<Page<SubmissionListItem>>(`/contests/${id}/my-submissions`, { ...params }),
};

// ---- Blogs ----
export const blogApi = {
  list: (params: PageParams) =>
    api.get<Page<BlogListItem>>("/blogs", { ...params }),
  get: (id: number) => api.get<BlogDetail>(`/blogs/${id}`),
  create: (body: { title: string; content: string }) =>
    api.post<BlogDetail>("/blogs", body),
  update: (id: number, body: { title?: string; content?: string }) =>
    api.put<BlogDetail>(`/blogs/${id}`, body),
  vote: (id: number, voteType: "UPVOTE" | "DOWNVOTE") =>
    api.post<string>(`/blogs/${id}/vote`, { voteType }),
  addComment: (id: number, content: string, parentId: number | null) =>
    api.post<BlogComment>(`/blogs/${id}/comments`, { content, parentId }),
};

// ---- Users ----
export const userApi = {
  get: (username: string) => api.get<UserProfile>(`/users/${username}`),
  submissions: (username: string, params: PageParams) =>
    api.get<Page<SubmissionListItem>>(`/users/${username}/submissions`, { ...params }),
  ratings: (params: PageParams) =>
    api.get<Page<UserProfile>>("/users/ratings", { ...params }),
  updateMe: (avatarUrl: string) =>
    api.put<UserProfile>("/users/me", { avatarUrl }),
};
