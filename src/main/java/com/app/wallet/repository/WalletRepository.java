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
}