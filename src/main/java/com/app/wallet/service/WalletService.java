package com.app.wallet.service;

import com.app.wallet.dto.BalanceResponseDto;
import com.app.wallet.dto.PageResponseDto;
import com.app.wallet.dto.TransactionResponseDto;
import com.app.wallet.dto.WalletResponseDto;
import com.app.wallet.exception.BadRequestException;
import com.app.wallet.exception.ConflictException;
import com.app.wallet.exception.InsufficientBalanceException;
import com.app.wallet.exception.ResourceNotFoundException;
import com.app.wallet.model.TransactionType;
import com.app.wallet.model.User;
import com.app.wallet.model.Wallet;
import com.app.wallet.model.WalletTransaction;
import com.app.wallet.repository.UserRepository;
import com.app.wallet.repository.WalletRepository;
import com.app.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every balance change runs in one database transaction that first takes a row lock on the
 * wallet(s) involved (SELECT ... FOR UPDATE), so concurrent requests are serialized and the
 * balance can never be double-spent. A transfer locks both wallets in ascending id order to
 * avoid deadlocks. Any exception rolls back the balance updates and ledger rows together.
 */
@Service
public class WalletService {

    static final String STATUS_COMPLETED = "COMPLETED";
    static final String WALLET_ACTIVE = "ACTIVE";
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;
    private static final int MAX_BALANCE_PRECISION = 18;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository,
                         UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponseDto getWallet(User user) {
        return WalletResponseDto.from(findWallet(user.getId()));
    }

    @Transactional(readOnly = true)
    public BalanceResponseDto getBalance(User user) {
        return new BalanceResponseDto(findWallet(user.getId()).getBalance());
    }

    @Transactional
    public TransactionResponseDto deposit(User user, BigDecimal amount, String description, String idempotencyKey) {

        validateKey(idempotencyKey);
        Wallet wallet = lock(findWallet(user.getId()).getId());

        Optional<WalletTransaction> replay = replay(wallet, idempotencyKey, TransactionType.DEPOSIT, amount, null);
        if (replay.isPresent()) {
            return TransactionResponseDto.from(replay.get());
        }

        BigDecimal newBalance = wallet.getBalance().add(scaled(amount));
        assertWithinLimit(newBalance);

        return TransactionResponseDto.from(apply(
                wallet, newBalance, TransactionType.DEPOSIT, amount, null,
                UUID.randomUUID().toString(), idempotencyKey, description));
    }

    @Transactional
    public TransactionResponseDto withdraw(User user, BigDecimal amount, String description, String idempotencyKey) {

        validateKey(idempotencyKey);
        Wallet wallet = lock(findWallet(user.getId()).getId());

        Optional<WalletTransaction> replay = replay(wallet, idempotencyKey, TransactionType.WITHDRAWAL, amount, null);
        if (replay.isPresent()) {
            return TransactionResponseDto.from(replay.get());
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        return TransactionResponseDto.from(apply(
                wallet, wallet.getBalance().subtract(scaled(amount)), TransactionType.WITHDRAWAL, amount, null,
                UUID.randomUUID().toString(), idempotencyKey, description));
    }

    @Transactional
    public TransactionResponseDto transfer(User user, String recipientEmail, BigDecimal amount,
                                           String description, String idempotencyKey) {

        validateKey(idempotencyKey);

        Wallet senderUnlocked = findWallet(user.getId());

        User recipientUser = userRepository.findUserByEmail(recipientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient [" + recipientEmail + "] not found"));

        if (recipientUser.getId().equals(user.getId())) {
            throw new BadRequestException("You cannot transfer money to yourself");
        }
        if (!recipientUser.isActive()) {
            throw new ConflictException("Recipient account is deactivated");
        }

        Wallet recipientUnlocked = findWallet(recipientUser.getId());

        // always lock in ascending id order so two opposite transfers cannot deadlock
        Wallet first = lock(Math.min(senderUnlocked.getId(), recipientUnlocked.getId()));
        Wallet second = lock(Math.max(senderUnlocked.getId(), recipientUnlocked.getId()));
        Wallet sender = first.getId().equals(senderUnlocked.getId()) ? first : second;
        Wallet recipient = sender == first ? second : first;

        Optional<WalletTransaction> replay =
                replay(sender, idempotencyKey, TransactionType.TRANSFER_OUT, amount, recipient.getId());
        if (replay.isPresent()) {
            return TransactionResponseDto.from(replay.get());
        }

        if (!WALLET_ACTIVE.equals(recipient.getStatus())) {
            throw new ConflictException("Recipient wallet is not active");
        }
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        BigDecimal recipientBalance = recipient.getBalance().add(scaled(amount));
        assertWithinLimit(recipientBalance);

        String reference = UUID.randomUUID().toString();

        WalletTransaction outgoing = apply(
                sender, sender.getBalance().subtract(scaled(amount)), TransactionType.TRANSFER_OUT,
                amount, recipient.getId(), reference, idempotencyKey, description);

        apply(recipient, recipientBalance, TransactionType.TRANSFER_IN,
                amount, sender.getId(), reference, null, description);

        return TransactionResponseDto.from(outgoing);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<TransactionResponseDto> getTransactions(User user, int page, int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Wallet wallet = findWallet(user.getId());

        List<TransactionResponseDto> content = transactionRepository
                .findByWalletId(wallet.getId(), safeSize, safePage * safeSize)
                .stream()
                .map(TransactionResponseDto::from)
                .toList();

        return new PageResponseDto<>(content, safePage, safeSize, transactionRepository.countByWalletId(wallet.getId()));
    }

    private WalletTransaction apply(Wallet wallet, BigDecimal newBalance, TransactionType type,
                                    BigDecimal amount, Long counterpartyWalletId, String reference,
                                    String idempotencyKey, String description) {

        if (!WALLET_ACTIVE.equals(wallet.getStatus())) {
            throw new ConflictException("Wallet is not active");
        }

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setCounterpartyWalletId(counterpartyWalletId);
        tx.setType(type);
        tx.setAmount(scaled(amount));
        tx.setBalanceBefore(wallet.getBalance());
        tx.setBalanceAfter(newBalance);
        tx.setStatus(STATUS_COMPLETED);
        tx.setReference(reference);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setDescription(description);
        // match database timestamp precision so a replayed response is identical to the original
        tx.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));

        walletRepository.updateBalance(wallet.getId(), newBalance);
        return transactionRepository.insert(tx);
    }

    /**
     * Returns the stored result when this idempotency key was already used on this wallet.
     * Reusing a key for a different request is a client bug and is rejected.
     */
    private Optional<WalletTransaction> replay(Wallet wallet, String key, TransactionType type,
                                               BigDecimal amount, Long counterpartyWalletId) {

        if (key == null) {
            return Optional.empty();
        }

        Optional<WalletTransaction> existing = transactionRepository.findByIdempotencyKey(wallet.getId(), key);

        existing.ifPresent(tx -> {
            boolean same = tx.getType() == type
                    && tx.getAmount().compareTo(amount) == 0
                    && java.util.Objects.equals(tx.getCounterpartyWalletId(), counterpartyWalletId);
            if (!same) {
                throw new ConflictException("Idempotency key was already used for a different request");
            }
        });

        return existing;
    }

    private Wallet findWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private Wallet lock(Long walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private void validateKey(String key) {
        if (key != null && (key.isBlank() || key.length() > MAX_IDEMPOTENCY_KEY_LENGTH)) {
            throw new BadRequestException(
                    "Idempotency-Key must be between 1 and " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }
    }

    private void assertWithinLimit(BigDecimal balance) {
        if (balance.precision() > MAX_BALANCE_PRECISION) {
            throw new BadRequestException("Resulting balance exceeds the maximum supported amount");
        }
    }

    private BigDecimal scaled(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }
}
