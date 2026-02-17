CREATE TABLE problems (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) UNIQUE NOT NULL,
    statement       TEXT NOT NULL,
    input_format    TEXT,
    output_format   TEXT,
    difficulty      VARCHAR(20),
    time_limit_ms   INT NOT NULL DEFAULT 2000,
    memory_limit_mb INT NOT NULL DEFAULT 256,
    author_id       BIGINT NOT NULL REFERENCES users(id),
    is_published    BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_problems_slug ON problems (slug);
CREATE INDEX idx_problems_author ON problems (author_id);
CREATE INDEX idx_problems_difficulty ON problems (difficulty);
CREATE INDEX idx_problems_published ON problems (is_published);

CREATE TABLE tags (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE problem_tags (
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    tag_id     BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (problem_id, tag_id)
);

CREATE TABLE test_cases (
    id                  BIGSERIAL PRIMARY KEY,
    problem_id          BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    input_url           VARCHAR(500),
    expected_output_url VARCHAR(500),
    is_sample           BOOLEAN NOT NULL DEFAULT false,
    order_index         INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_test_cases_problem ON test_cases (problem_id);
