CREATE TABLE IF NOT EXISTS slack_installations (
    slack_installation_id BIGSERIAL PRIMARY KEY,
    workspace_id VARCHAR(32) NOT NULL UNIQUE,
    enterprise_id VARCHAR(32),
    bot_user_id VARCHAR(32) NOT NULL,
    bot_access_token_encrypted TEXT NOT NULL,
    scopes TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS slack_connections (
    slack_connection_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    slack_installation_id BIGINT NOT NULL REFERENCES slack_installations(slack_installation_id) ON DELETE RESTRICT,
    workspace_id VARCHAR(32) NOT NULL,
    slack_user_id VARCHAR(32) NOT NULL,
    dm_channel_id VARCHAR(32),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_slack_connection_workspace_user UNIQUE (workspace_id, slack_user_id)
);

CREATE INDEX IF NOT EXISTS idx_slack_connections_installation
    ON slack_connections(slack_installation_id);
