package com.app.wallet.repository;

import com.app.wallet.model.TransactionType;
import com.app.wallet.model.WalletTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class WalletTransactionRepository {

    private static final RowMapper<WalletTransaction> MAPPER = (rs, rowNum) -> {
        WalletTransaction tx = new WalletTransaction();
        tx.setId(rs.getLong("id"));
        tx.setWalletId(rs.getLong("wallet_id"));
        long counterparty = rs.getLong("counterparty_wallet_id");
        tx.setCounterpartyWalletId(rs.wasNull() ? null : counterparty);
        tx.setType(TransactionType.valueOf(rs.getString("type")));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setBalanceBefore(rs.getBigDecimal("balance_before"));
        tx.setBalanceAfter(rs.getBigDecimal("balance_after"));
        tx.setStatus(rs.getString("status"));
        tx.setReference(rs.getString("reference"));
        tx.setIdempotencyKey(rs.getString("idempotency_key"));
        tx.setDescription(rs.getString("description"));
        tx.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return tx;
    };

    private final JdbcTemplate jdbcTemplate;

    public WalletTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public WalletTransaction insert(WalletTransaction tx) {

        String sql = """
            INSERT INTO wallet_transactions
            (wallet_id, counterparty_wallet_id, type, amount, balance_before, balance_after,
             status, reference, idempotency_key, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, tx.getWalletId());
            if (tx.getCounterpartyWalletId() == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, tx.getCounterpartyWalletId());
            }
            ps.setString(3, tx.getType().name());
            ps.setBigDecimal(4, tx.getAmount());
            ps.setBigDecimal(5, tx.getBalanceBefore());
            ps.setBigDecimal(6, tx.getBalanceAfter());
            ps.setString(7, tx.getStatus());
            ps.setString(8, tx.getReference());
            ps.setString(9, tx.getIdempotencyKey());
            ps.setString(10, tx.getDescription());
            ps.setTimestamp(11, Timestamp.valueOf(tx.getCreatedAt()));
            return ps;
        }, keyHolder);

        tx.setId(keyHolder.getKey().longValue());
        return tx;
    }

    public Optional<WalletTransaction> findByIdempotencyKey(Long walletId, String key) {

        return jdbcTemplate
                .query("SELECT * FROM wallet_transactions WHERE wallet_id = ? AND idempotency_key = ?",
                        MAPPER, walletId, key)
                .stream()
                .findFirst();
    }

    public List<WalletTransaction> findByWalletId(Long walletId, int limit, int offset) {

        String sql = """
            SELECT *
            FROM wallet_transactions
            WHERE wallet_id = ?
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """;

        return jdbcTemplate.query(sql, MAPPER, walletId, limit, offset);
    }

    public long countByWalletId(Long walletId) {

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_transactions WHERE wallet_id = ?",
                Long.class,
                walletId
        );

        return count == null ? 0 : count;
    }
}
