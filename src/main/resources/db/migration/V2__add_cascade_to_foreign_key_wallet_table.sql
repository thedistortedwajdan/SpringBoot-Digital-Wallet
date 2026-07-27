-- 1. Drop the existing foreign key constraint
ALTER TABLE wallets
DROP CONSTRAINT fk_wallet_user;

-- 2. Re-create the foreign key with ON DELETE CASCADE
ALTER TABLE wallets
ADD CONSTRAINT fk_wallet_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE CASCADE;
