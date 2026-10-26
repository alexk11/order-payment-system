-- changeset init-orders-:001

-- TODO остальные поля, необходимые индексы
CREATE TABLE IF NOT EXISTS orders
(
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),
    customer_id         INTEGER NOT NULL,
    client_estimate     NUMERIC,
    final_amount        NUMERIC,
    authorized_amount   NUMERIC,
    captured_amount     NUMERIC,
    payment_status      INTEGER,
    address             TEXT,
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);
