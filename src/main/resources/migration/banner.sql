CREATE TABLE IF NOT EXISTS banner
(
    id BIGSERIAL PRIMARY KEY,
    alias VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    mobile_image_path TEXT,
    pc_image_path TEXT NOT NULL,
    mobile_html_path TEXT,
    pc_html_path TEXT,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NOT NULL,
    order_no INTEGER NOT NULL DEFAULT 0,
    show BOOLEAN NOT NULL DEFAULT TRUE,
    last_modified_admin_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_banner_display
    ON banner (show, started_at, ended_at, order_no, id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_banner_alias_active
    ON banner (alias)
    WHERE deleted_at IS NULL;
