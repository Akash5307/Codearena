CREATE TABLE submissions (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id),
    problem_id        BIGINT NOT NULL REFERENCES problems(id),
    contest_id        BIGINT REFERENCES contests(id),
    language          VARCHAR(20) NOT NULL,
    source_code       TEXT NOT NULL,
    verdict           VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    time_used_ms      INT,
    memory_used_kb    INT,
    test_cases_passed INT NOT NULL DEFAULT 0,
    total_test_cases  INT NOT NULL DEFAULT 0,
    submitted_at      TIMESTAMP NOT NULL DEFAULT now(),
    judged_at         TIMESTAMP
);

CREATE INDEX idx_submissions_user ON submissions (user_id);
CREATE INDEX idx_submissions_problem ON submissions (problem_id);
CREATE INDEX idx_submissions_contest ON submissions (contest_id);
CREATE INDEX idx_submissions_verdict ON submissions (verdict);
CREATE INDEX idx_submissions_user_problem ON submissions (user_id, problem_id);
CREATE INDEX idx_submissions_submitted_at ON submissions (submitted_at DESC);
