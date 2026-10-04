package com.app.wallet.repository;

import com.app.wallet.model.Wallet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public class WalletRepository {

    private static final RowMapper<Wallet> WALLET_MAPPER = (rs, rowNum) -> {
        Wallet wallet = new Wallet();
        wallet.setId(rs.getLong("id"));
        wallet.setUserId(rs.getLong("user_id"));
        wallet.setBalance(rs.getBigDecimal("balance"));
        wallet.setStatus(rs.getString("status"));
        wallet.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return wallet;
    };

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

    public Optional<Wallet> findByUserId(Long userId) {

        return jdbcTemplate
                .query("SELECT * FROM wallets WHERE user_id = ?", WALLET_MAPPER, userId)
                .stream()
                .findFirst();
    }

    /**
     * Reads the wallet and takes a row lock until the surrounding transaction ends, so
     * concurrent balance changes on the same wallet are serialized.
     */
    public Optional<Wallet> findByIdForUpdate(Long walletId) {

        return jdbcTemplate
                .query("SELECT * FROM wallets WHERE id = ? FOR UPDATE", WALLET_MAPPER, walletId)
                .stream()
                .findFirst();
    }

    public void updateBalance(Long walletId, BigDecimal balance) {

        jdbcTemplate.update(
                "UPDATE wallets SET balance = ? WHERE id = ?",
                balance,
                walletId
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
