-- ============================================================
-- Task Manager Database
-- PostgreSQL
-- ============================================================

-- ============================================================
-- users
-- ============================================================

CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_username
        UNIQUE (username),

    CONSTRAINT uq_users_email
        UNIQUE (email)
);


-- ============================================================
-- tasks
-- ============================================================

CREATE TABLE tasks (
    task_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tasks_user
        FOREIGN KEY (user_id)
        REFERENCES users (user_id),

    CONSTRAINT ck_tasks_status
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),

    CONSTRAINT ck_tasks_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);


-- ============================================================
-- tags
-- ============================================================

CREATE TABLE tags (
    tag_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tags_user
        FOREIGN KEY (user_id)
        REFERENCES users (user_id),

    CONSTRAINT uq_tags_user_name
        UNIQUE (user_id, name)
);


-- ============================================================
-- task_tags
-- ============================================================

CREATE TABLE task_tags (
    task_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,

    CONSTRAINT pk_task_tags
        PRIMARY KEY (task_id, tag_id),

    CONSTRAINT fk_task_tags_task
        FOREIGN KEY (task_id)
        REFERENCES tasks (task_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_tags_tag
        FOREIGN KEY (tag_id)
        REFERENCES tags (tag_id)
        ON DELETE CASCADE
);


-- ============================================================
-- Indexes
-- ============================================================

CREATE INDEX idx_tasks_user_id
    ON tasks (user_id);

CREATE INDEX idx_tasks_due_date
    ON tasks (due_date);

CREATE INDEX idx_tags_user_id
    ON tags (user_id);