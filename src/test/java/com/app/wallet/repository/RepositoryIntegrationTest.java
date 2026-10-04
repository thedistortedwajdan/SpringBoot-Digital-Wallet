package com.app.wallet.repository;

import com.app.wallet.model.TransactionType;
import com.app.wallet.model.User;
import com.app.wallet.model.Wallet;
import com.app.wallet.model.WalletTransaction;
import com.app.wallet.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletTransactionRepository transactionRepository;

    private long createUser(String email) {
        User user = new User();
        user.setFirstName("F");
        user.setLastName("L");
        user.setEmail(email);
        user.setPassword("hash");
        user.setRole("USER");
        long id = userRepository.createUser(user);
        walletRepository.createWallet(id);
        return id;
    }

    private WalletTransaction tx(long walletId, String key, String amount) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(walletId);
        tx.setType(TransactionType.DEPOSIT);
        tx.setAmount(new BigDecimal(amount));
        tx.setBalanceBefore(BigDecimal.ZERO);
        tx.setBalanceAfter(new BigDecimal(amount));
        tx.setStatus("COMPLETED");
        tx.setReference("ref-" + amount);
        tx.setIdempotencyKey(key);
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }

    @Test
    void userQueries_findByEmailAndIdAndExists() {
        long id = createUser("a@test.com");

        assertThat(userRepository.findUserByEmail("a@test.com")).get()
                .satisfies(u -> {
                    assertThat(u.getId()).isEqualTo(id);
                    assertThat(u.isActive()).isTrue();
                    assertThat(u.getRole()).isEqualTo("USER");
                });
        assertThat(userRepository.findUserById(id)).isPresent();
        assertThat(userRepository.existsByEmail("a@test.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@test.com")).isFalse();
        assertThat(userRepository.findUserByEmail("none@test.com")).isEmpty();
    }

    @Test
    void duplicateEmail_violatesUniqueConstraint() {
        createUser("a@test.com");

        assertThatThrownBy(() -> createUser("a@test.com"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void invalidRole_isRejectedByDatabase() {
        User user = new User();
        user.setFirstName("F");
        user.setLastName("L");
        user.setEmail("a@test.com");
        user.setPassword("hash");
        user.setRole("SUPERUSER");

        assertThatThrownBy(() -> userRepository.createUser(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userPaging_ordersByIdAndCounts() {
        createUser("a@test.com");
        createUser("b@test.com");
        createUser("c@test.com");

        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(userRepository.findAll(2, 0)).extracting(User::getEmail)
                .containsExactly("a@test.com", "b@test.com");
        assertThat(userRepository.findAll(2, 2)).extracting(User::getEmail)
                .containsExactly("c@test.com");
    }

    @Test
    void userUpdates_changeRoleStatusAndPassword() {
        long id = createUser("a@test.com");

        userRepository.updateRole(id, "ADMIN");
        userRepository.updateActive(id, false);
        userRepository.updatePassword(id, "newhash");

        User user = userRepository.findUserById(id).orElseThrow();
        assertThat(user.getRole()).isEqualTo("ADMIN");
        assertThat(user.isActive()).isFalse();
        assertThat(user.getPassword()).isEqualTo("newhash");
    }

    @Test
    void walletQueries_createFindAndUpdateBalance() {
        long userId = createUser("a@test.com");

        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");
        assertThat(wallet.getStatus()).isEqualTo("ACTIVE");

        walletRepository.updateBalance(wallet.getId(), new BigDecimal("12.34"));

        assertThat(walletRepository.findByIdForUpdate(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("12.34");
        assertThat(walletRepository.findByUserId(999_999L)).isEmpty();
    }

    @Test
    void negativeBalance_isRejectedByDatabase() {
        long userId = createUser("a@test.com");
        long walletId = walletRepository.findByUserId(userId).orElseThrow().getId();

        assertThatThrownBy(() -> walletRepository.updateBalance(walletId, new BigDecimal("-0.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void walletActivity_isDetectedFromBalanceOrLedger() {
        long userId = createUser("a@test.com");
        long walletId = walletRepository.findByUserId(userId).orElseThrow().getId();
        assertThat(walletRepository.hasActivity(userId)).isFalse();

        transactionRepository.insert(tx(walletId, null, "5.00"));

        assertThat(walletRepository.hasActivity(userId)).isTrue();
    }

    @Test
    void transactionQueries_pageNewestFirstAndFindByKey() {
        long userId = createUser("a@test.com");
        long walletId = walletRepository.findByUserId(userId).orElseThrow().getId();

        WalletTransaction first = tx(walletId, "k1", "1.00");
        first.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        transactionRepository.insert(first);
        transactionRepository.insert(tx(walletId, "k2", "2.00"));

        assertThat(transactionRepository.countByWalletId(walletId)).isEqualTo(2);
        assertThat(transactionRepository.findByWalletId(walletId, 10, 0))
                .extracting(WalletTransaction::getIdempotencyKey)
                .containsExactly("k2", "k1");
        assertThat(transactionRepository.findByIdempotencyKey(walletId, "k1")).isPresent();
        assertThat(transactionRepository.findByIdempotencyKey(walletId, "missing")).isEmpty();
    }

    @Test
    void idempotencyKey_isUniquePerWallet() {
        long userId = createUser("a@test.com");
        long walletId = walletRepository.findByUserId(userId).orElseThrow().getId();
        transactionRepository.insert(tx(walletId, "same", "1.00"));

        assertThatThrownBy(() -> transactionRepository.insert(tx(walletId, "same", "2.00")))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
