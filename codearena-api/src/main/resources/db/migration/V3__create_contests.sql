CREATE TABLE contests (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    slug             VARCHAR(255) UNIQUE NOT NULL,
    description      TEXT,
    type             VARCHAR(20) NOT NULL,
    start_time       TIMESTAMP NOT NULL,
    duration_minutes INT NOT NULL,
    is_rated         BOOLEAN NOT NULL DEFAULT true,
    author_id        BIGINT NOT NULL REFERENCES users(id),
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_contests_slug ON contests (slug);
CREATE INDEX idx_contests_start_time ON contests (start_time);

CREATE TABLE contest_problems (
    id           BIGSERIAL PRIMARY KEY,
    contest_id   BIGINT NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    problem_id   BIGINT NOT NULL REFERENCES problems(id),
    label        VARCHAR(5) NOT NULL,
    order_index  INT NOT NULL DEFAULT 0,
    points       INT,
    UNIQUE (contest_id, problem_id),
    UNIQUE (contest_id, label)
);

CREATE TABLE contest_registrations (
    id            BIGSERIAL PRIMARY KEY,
    contest_id    BIGINT NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    registered_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (contest_id, user_id)
);

CREATE INDEX idx_contest_registrations_contest ON contest_registrations (contest_id);
CREATE INDEX idx_contest_registrations_user ON contest_registrations (user_id);
