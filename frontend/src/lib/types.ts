// TS mirrors of backend DTOs (com.codearena.*.dto). Keep in sync with CLAUDE.md.

export interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  errorCode: string | null;
  error: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page, 0-based
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export type Role = "USER" | "PROBLEM_SETTER" | "ADMIN";

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  role: Role;
  rating: number;
  maxRating: number;
  avatarUrl: string | null;
  createdAt: string;
}

export type Difficulty = "EASY" | "MEDIUM" | "HARD" | string;

export interface ProblemListItem {
  id: number;
  title: string;
  slug: string;
  difficulty: Difficulty | null;
  timeLimitMs: number;
  memoryLimitMb: number;
  authorUsername: string;
  tags: string[];
  createdAt: string;
}

export interface SampleTestCase {
  id: number;
  inputUrl: string;
  expectedOutputUrl: string;
  isSample: boolean;
  orderIndex: number;
}

// Actual sample input/output text, from GET /problems/{slug}/samples
export interface SampleTestCaseContent {
  id: number;
  orderIndex: number;
  input: string;
  output: string;
}

export interface ProblemDetail {
  id: number;
  title: string;
  slug: string;
  statement: string;
  inputFormat: string | null;
  outputFormat: string | null;
  difficulty: Difficulty | null;
  timeLimitMs: number;
  memoryLimitMb: number;
  authorUsername: string;
  isPublished: boolean;
  tags: string[];
  sampleTestCases: SampleTestCase[];
  createdAt: string;
  updatedAt: string;
}

export interface Tag {
  id: number;
  name: string;
}

export const LANGUAGES = [
  "CPP",
  "JAVA",
  "C",
  "PYTHON",
  "JAVASCRIPT",
  "GO",
  "RUST",
  "KOTLIN",
] as const;
export type Language = (typeof LANGUAGES)[number];

export const LANGUAGE_LABELS: Record<Language, string> = {
  CPP: "C++",
  JAVA: "Java",
  C: "C",
  PYTHON: "Python",
  JAVASCRIPT: "JavaScript",
  GO: "Go",
  RUST: "Rust",
  KOTLIN: "Kotlin",
};

export type Verdict =
  | "PENDING"
  | "JUDGING"
  | "AC"
  | "WA"
  | "TLE"
  | "MLE"
  | "RE"
  | "CE"
  | string;

export interface SubmissionListItem {
  id: number;
  username: string;
  problemId: number;
  problemTitle: string;
  contestId: number | null;
  language: Language;
  verdict: Verdict;
  timeUsedMs: number | null;
  memoryUsedKb: number | null;
  submittedAt: string;
}

export interface SubmissionDetail {
  id: number;
  username: string;
  problemId: number;
  problemTitle: string;
  contestId: number | null;
  language: Language;
  sourceCode: string;
  verdict: Verdict;
  timeUsedMs: number | null;
  memoryUsedKb: number | null;
  testCasesPassed: number;
  totalTestCases: number;
  submittedAt: string;
  judgedAt: string | null;
}

export interface SubmitRequest {
  problemId: number;
  contestId?: number | null;
  language: Language;
  sourceCode: string;
}

// ---- Contests ----
export type ContestState = "BEFORE" | "RUNNING" | "ENDED" | string;

export interface ContestListItem {
  id: number;
  title: string;
  slug: string;
  type: string;
  state: ContestState;
  startTime: string;
  durationMinutes: number;
  isRated: boolean;
  authorUsername: string;
  createdAt: string;
}

export interface ContestProblemItem {
  problemId: number;
  problemSlug: string;
  problemTitle: string;
  label: string;
  orderIndex: number;
  points: number | null;
}

export interface ContestDetail {
  id: number;
  title: string;
  slug: string;
  description: string | null;
  type: string;
  state: ContestState;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  isRated: boolean;
  authorUsername: string;
  registrationCount: number;
  problems: ContestProblemItem[];
  createdAt: string;
  updatedAt: string;
}

export interface StandingsProblemResult {
  label: string;
  solved: boolean;
  attempts: number;
  solvedAtMinute: number | null;
}

export interface StandingsEntry {
  rank: number;
  userId: number;
  username: string;
  solvedCount: number;
  penaltyMinutes: number;
  problemResults: StandingsProblemResult[];
}

export interface Standings {
  contestId: number;
  contestTitle: string;
  entries: StandingsEntry[];
}

// ---- Blogs ----
export interface BlogListItem {
  id: number;
  title: string;
  authorUsername: string;
  upvotes: number;
  downvotes: number;
  commentCount: number;
  createdAt: string;
}

export interface BlogComment {
  id: number;
  authorUsername: string;
  content: string;
  parentId: number | null;
  replies: BlogComment[];
  createdAt: string;
  updatedAt: string;
}

export interface BlogDetail {
  id: number;
  title: string;
  content: string;
  authorUsername: string;
  upvotes: number;
  downvotes: number;
  comments: BlogComment[];
  createdAt: string;
  updatedAt: string;
}
