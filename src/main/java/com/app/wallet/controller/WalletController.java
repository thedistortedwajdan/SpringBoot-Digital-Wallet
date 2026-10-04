package com.app.wallet.controller;

import com.app.wallet.dto.BalanceResponseDto;
import com.app.wallet.dto.MoneyRequestDto;
import com.app.wallet.dto.PageResponseDto;
import com.app.wallet.dto.TransactionResponseDto;
import com.app.wallet.dto.TransferRequestDto;
import com.app.wallet.dto.WalletResponseDto;
import com.app.wallet.service.AuthenticatedUserProvider;
import com.app.wallet.service.WalletService;
import jakarta.validation.Valid;
import com.app.wallet.config.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Wallet")
@ApiErrorResponses
@RequestMapping("/api/wallet")
public class WalletController {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final WalletService walletService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public WalletController(WalletService walletService,
                            AuthenticatedUserProvider authenticatedUserProvider) {
        this.walletService = walletService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Operation(summary = "Get the authenticated user's wallet")
    @GetMapping
    public WalletResponseDto getWallet() {
        return walletService.getWallet(authenticatedUserProvider.getAuthenticatedUser());
    }

    @Operation(summary = "Get the wallet balance")
    @GetMapping("/balance")
    public BalanceResponseDto getBalance() {
        return walletService.getBalance(authenticatedUserProvider.getAuthenticatedUser());
    }

    @Operation(summary = "Deposit money")
    @PostMapping("/deposit")
    public TransactionResponseDto deposit(
            @Valid @RequestBody MoneyRequestDto request,
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String idempotencyKey) {
        return walletService.deposit(
                authenticatedUserProvider.getAuthenticatedUser(),
                request.amount(), request.description(), idempotencyKey);
    }

    @Operation(summary = "Withdraw money (fails if balance is insufficient)")
    @PostMapping("/withdraw")
    public TransactionResponseDto withdraw(
            @Valid @RequestBody MoneyRequestDto request,
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String idempotencyKey) {
        return walletService.withdraw(
                authenticatedUserProvider.getAuthenticatedUser(),
                request.amount(), request.description(), idempotencyKey);
    }

    @Operation(summary = "Transfer money to another user by email")
    @PostMapping("/transfer")
    public TransactionResponseDto transfer(
            @Valid @RequestBody TransferRequestDto request,
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String idempotencyKey) {
        return walletService.transfer(
                authenticatedUserProvider.getAuthenticatedUser(),
                request.recipientEmail(), request.amount(), request.description(), idempotencyKey);
    }

    @Operation(summary = "List wallet transactions, newest first")
    @GetMapping("/transactions")
    public PageResponseDto<TransactionResponseDto> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return walletService.getTransactions(
                authenticatedUserProvider.getAuthenticatedUser(), page, size);
    }
}
