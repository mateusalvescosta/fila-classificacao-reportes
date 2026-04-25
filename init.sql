CREATE TABLE IF NOT EXISTS tasks (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_name  VARCHAR(100) NOT NULL,
    payload     JSONB        NOT NULL,
    result      JSONB,
    priority    INT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'pending',
    attempts    INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);