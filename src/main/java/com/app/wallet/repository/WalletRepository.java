package com.app.wallet.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class WalletRepository {

    private final JdbcTemplate jdbcTemplate;

    public WalletRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createWallet(Long userId) {

        String sql = """
            INSERT INTO wallets(user_id, balance)
            VALUES (?, ?)
            """;

        jdbcTemplate.update(
                sql,
                userId,
                BigDecimal.ZERO
        );
    }

    /**
     * True when the user's wallet holds money or has ever been part of a transaction,
     * in which case the account must be kept for the ledger.
     */
    public boolean hasActivity(Long userId) {

        String sql = """
            SELECT COUNT(*)
            FROM wallets w
            WHERE w.user_id = ?
              AND (w.balance <> 0
                   OR EXISTS (SELECT 1 FROM wallet_transactions t
                              WHERE t.wallet_id = w.id OR t.counterparty_wallet_id = w.id))
            """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);

        return count != null && count > 0;
    }
}
