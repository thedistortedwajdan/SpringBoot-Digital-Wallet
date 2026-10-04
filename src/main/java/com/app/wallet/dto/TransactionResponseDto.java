package com.app.wallet.dto;

import com.app.wallet.model.TransactionType;
import com.app.wallet.model.WalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDto(
        Long id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String status,
        String reference,
        Long counterpartyWalletId,
        String description,
        LocalDateTime createdAt) {

    public static TransactionResponseDto from(WalletTransaction tx) {
        return new TransactionResponseDto(
                tx.getId(),
                tx.getType(),
                tx.getAmount(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getStatus(),
                tx.getReference(),
                tx.getCounterpartyWalletId(),
                tx.getDescription(),
                tx.getCreatedAt());
    }
}
