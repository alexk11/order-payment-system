CREATE TABLE IF NOT EXISTS tasks
(
    id              BIGSERIAL PRIMARY KEY,
    order_id        UUID      NOT NULL,
    status          INTEGER     NOT NULL,
    in_processing   BOOLEAN     NOT NULL DEFAULT false,
    attempts        INTEGER,
    next_attempt_at TIMESTAMPTZ,
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tasks_order_id ON tasks (order_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status_next_attempt ON tasks (status, next_attempt_at);

ALTER TABLE tasks ADD CONSTRAINT fk_order_id FOREIGN KEY (order_id) REFERENCES orders (id) ON UPDATE NO ACTION ON DELETE NO ACTION;
