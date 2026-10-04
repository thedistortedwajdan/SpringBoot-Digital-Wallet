-- Ledger of every balance movement, one row per wallet affected.
-- A transfer writes two rows (TRANSFER_OUT / TRANSFER_IN) sharing the same reference.
CREATE TABLE wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    counterparty_wallet_id BIGINT,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    balance_before DECIMAL(18,2) NOT NULL,
    balance_after DECIMAL(18,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    reference VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(100),
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wallet_tx_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_wallet_tx_counterparty
        FOREIGN KEY (counterparty_wallet_id) REFERENCES wallets(id),
    CONSTRAINT chk_wallet_tx_type
        CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT')),
    CONSTRAINT chk_wallet_tx_amount CHECK (amount > 0),
    CONSTRAINT uq_wallet_tx_idempotency UNIQUE (wallet_id, idempotency_key)
);

CREATE INDEX idx_wallet_tx_wallet_created
    ON wallet_transactions (wallet_id, created_at DESC, id DESC);

-- Last line of defence against overdrafts, whatever the application does.
ALTER TABLE wallets
ADD CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0);

-- Superseded by wallet_transactions; was never written to.
DROP TABLE transactions;
