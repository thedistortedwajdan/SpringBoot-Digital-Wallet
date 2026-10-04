package com.app.wallet.service;

import com.app.wallet.dto.TransactionResponseDto;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletService walletService;

    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("u" + id + "@test.com");
        return user;
    }

    private Wallet wallet(long id, long userId, String balance) {
        Wallet wallet = new Wallet();
        wallet.setId(id);
        wallet.setUserId(userId);
        wallet.setBalance(new BigDecimal(balance));
        wallet.setStatus("ACTIVE");
        wallet.setCreatedAt(LocalDateTime.now());
        return wallet;
    }

    private void givenWallet(Wallet wallet) {
        when(walletRepository.findByUserId(wallet.getUserId())).thenReturn(Optional.of(wallet));
        when(walletRepository.findByIdForUpdate(wallet.getId())).thenReturn(Optional.of(wallet));
    }

    private void echoInserts() {
        when(transactionRepository.insert(any(WalletTransaction.class))).thenAnswer(inv -> {
            WalletTransaction tx = inv.getArgument(0);
            tx.setId(1L);
            return tx;
        });
    }

    @Test
    void deposit_addsToBalanceAndRecordsBeforeAfter() {
        givenWallet(wallet(10, 1, "5.00"));
        echoInserts();

        TransactionResponseDto result = walletService.deposit(user(1), new BigDecimal("2.5"), "x", null);

        verify(walletRepository).updateBalance(10L, new BigDecimal("7.50"));
        assertThat(result.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(result.balanceBefore()).isEqualByComparingTo("5");
        assertThat(result.balanceAfter()).isEqualByComparingTo("7.5");
    }

    @Test
    void withdraw_withInsufficientBalance_writesNothing() {
        givenWallet(wallet(10, 1, "5.00"));

        assertThatThrownBy(() -> walletService.withdraw(user(1), new BigDecimal("5.01"), null, null))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletRepository, never()).updateBalance(anyLong(), any());
        verify(transactionRepository, never()).insert(any());
    }

    @Test
    void withdraw_exactBalance_isAllowed() {
        givenWallet(wallet(10, 1, "5.00"));
        echoInserts();

        walletService.withdraw(user(1), new BigDecimal("5.00"), null, null);

        verify(walletRepository).updateBalance(10L, new BigDecimal("0.00"));
    }

    @Test
    void deposit_replayWithSameKey_returnsStoredResultWithoutChangingBalance() {
        Wallet wallet = wallet(10, 1, "25.00");
        givenWallet(wallet);
        WalletTransaction stored = new WalletTransaction();
        stored.setId(99L);
        stored.setWalletId(10L);
        stored.setType(TransactionType.DEPOSIT);
        stored.setAmount(new BigDecimal("25.00"));
        stored.setBalanceBefore(BigDecimal.ZERO);
        stored.setBalanceAfter(new BigDecimal("25.00"));
        when(transactionRepository.findByIdempotencyKey(10L, "k")).thenReturn(Optional.of(stored));

        TransactionResponseDto result = walletService.deposit(user(1), new BigDecimal("25"), null, "k");

        assertThat(result.id()).isEqualTo(99L);
        verify(walletRepository, never()).updateBalance(anyLong(), any());
    }

    @Test
    void deposit_replayWithDifferentAmount_isConflict() {
        givenWallet(wallet(10, 1, "25.00"));
        WalletTransaction stored = new WalletTransaction();
        stored.setType(TransactionType.DEPOSIT);
        stored.setAmount(new BigDecimal("25.00"));
        when(transactionRepository.findByIdempotencyKey(10L, "k")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> walletService.deposit(user(1), new BigDecimal("30"), null, "k"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void blankOrOversizedIdempotencyKey_isRejected() {
        assertThatThrownBy(() -> walletService.deposit(user(1), BigDecimal.ONE, null, " "))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> walletService.deposit(user(1), BigDecimal.ONE, null, "k".repeat(101)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void transfer_debitsSenderCreditsRecipientAndLocksInIdOrder() {
        Wallet sender = wallet(20, 1, "100.00");
        Wallet recipient = wallet(10, 2, "50.00");
        givenWallet(sender);
        givenWallet(recipient);
        User recipientUser = user(2);
        recipientUser.setActive(true);
        when(userRepository.findUserByEmail("u2@test.com")).thenReturn(Optional.of(recipientUser));
        echoInserts();

        TransactionResponseDto result =
                walletService.transfer(user(1), "u2@test.com", new BigDecimal("70"), null, null);

        var order = inOrder(walletRepository);
        order.verify(walletRepository).findByIdForUpdate(10L);
        order.verify(walletRepository).findByIdForUpdate(20L);
        verify(walletRepository).updateBalance(20L, new BigDecimal("30.00"));
        verify(walletRepository).updateBalance(10L, new BigDecimal("120.00"));
        assertThat(result.type()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(result.counterpartyWalletId()).isEqualTo(10L);
    }

    @Test
    void transfer_toSelf_isRejected() {
        givenWalletForLookupOnly(wallet(10, 1, "100.00"));
        User self = user(1);
        self.setActive(true);
        when(userRepository.findUserByEmail("u1@test.com")).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> walletService.transfer(user(1), "u1@test.com", BigDecimal.TEN, null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void transfer_toUnknownRecipient_isNotFound() {
        givenWalletForLookupOnly(wallet(10, 1, "100.00"));
        when(userRepository.findUserByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.transfer(user(1), "nobody@test.com", BigDecimal.TEN, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transfer_withInsufficientBalance_writesNothing() {
        Wallet sender = wallet(10, 1, "5.00");
        Wallet recipient = wallet(20, 2, "0.00");
        givenWallet(sender);
        givenWallet(recipient);
        User recipientUser = user(2);
        recipientUser.setActive(true);
        when(userRepository.findUserByEmail("u2@test.com")).thenReturn(Optional.of(recipientUser));

        assertThatThrownBy(() -> walletService.transfer(user(1), "u2@test.com", BigDecimal.TEN, null, null))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletRepository, never()).updateBalance(anyLong(), any());
        verify(transactionRepository, never()).insert(any());
    }

    private void givenWalletForLookupOnly(Wallet wallet) {
        when(walletRepository.findByUserId(wallet.getUserId())).thenReturn(Optional.of(wallet));
    }
}
