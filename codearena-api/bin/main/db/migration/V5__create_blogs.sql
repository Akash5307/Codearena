CREATE TABLE blog_posts (
    id          BIGSERIAL PRIMARY KEY,
    author_id   BIGINT NOT NULL REFERENCES users(id),
    title       VARCHAR(255) NOT NULL,
    content     TEXT NOT NULL,
    upvotes     INT NOT NULL DEFAULT 0,
    downvotes   INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_blog_posts_author ON blog_posts (author_id);
CREATE INDEX idx_blog_posts_created ON blog_posts (created_at DESC);

CREATE TABLE comments (
    id           BIGSERIAL PRIMARY KEY,
    blog_post_id BIGINT NOT NULL REFERENCES blog_posts(id) ON DELETE CASCADE,
    author_id    BIGINT NOT NULL REFERENCES users(id),
    parent_id    BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    content      TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_blog_post ON comments (blog_post_id);
CREATE INDEX idx_comments_parent ON comments (parent_id);

CREATE TABLE blog_votes (
    id           BIGSERIAL PRIMARY KEY,
    blog_post_id BIGINT NOT NULL REFERENCES blog_posts(id) ON DELETE CASCADE,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    vote_type    VARCHAR(10) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (blog_post_id, user_id)
);

CREATE INDEX idx_blog_votes_post ON blog_votes (blog_post_id);
