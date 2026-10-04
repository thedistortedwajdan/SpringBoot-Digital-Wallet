package com.app.wallet.dto;

import com.app.wallet.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponseDto(
        Long id,
        BigDecimal balance,
        String status,
        LocalDateTime createdAt) {

    public static WalletResponseDto from(Wallet wallet) {
        return new WalletResponseDto(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getStatus(),
                wallet.getCreatedAt());
    }
}
