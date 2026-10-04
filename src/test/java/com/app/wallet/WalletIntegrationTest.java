package com.app.wallet;

import com.app.wallet.model.User;
import com.app.wallet.repository.UserRepository;
import com.app.wallet.service.WalletService;
import com.app.wallet.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletIntegrationTest extends IntegrationTestBase {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    private MockHttpServletRequestBuilder money(String path, String auth, String json) {
        return post(path)
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private BigDecimal balanceOf(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userId(email));
    }

    private void fund(String email, String amount) {
        jdbcTemplate.update("UPDATE wallets SET balance = ? WHERE user_id = ?",
                new BigDecimal(amount), userId(email));
    }

    @Test
    void walletEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/wallet")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/wallet/balance")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/wallet/deposit")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/wallet/transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void newUserHasEmptyWallet() throws Exception {
        String auth = registerAndLogin("a@test.com");

        mockMvc.perform(get("/api/wallet").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/wallet/balance").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void deposit_increasesBalanceAndWritesLedger() throws Exception {
        String auth = registerAndLogin("a@test.com");

        mockMvc.perform(money("/api/wallet/deposit", auth, "{\"amount\":100.50,\"description\":\"salary\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.balanceBefore").value(0))
                .andExpect(jsonPath("$.balanceAfter").value(100.5))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(balanceOf("a@test.com")).isEqualByComparingTo("100.50");
    }

    @Test
    void invalidAmounts_return400() throws Exception {
        String auth = registerAndLogin("a@test.com");

        for (String body : List.of(
                "{\"amount\":0}", "{\"amount\":-5}", "{\"amount\":1.234}", "{}", "{\"amount\":\"abc\"}")) {
            mockMvc.perform(money("/api/wallet/deposit", auth, body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(balanceOf("a@test.com")).isEqualByComparingTo("0");
    }

    @Test
    void withdraw_reducesBalance_andRejectsOverdraft() throws Exception {
        String auth = registerAndLogin("a@test.com");
        fund("a@test.com", "50.00");

        mockMvc.perform(money("/api/wallet/withdraw", auth, "{\"amount\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.balanceAfter").value(30));

        mockMvc.perform(money("/api/wallet/withdraw", auth, "{\"amount\":30.01}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance"));

        assertThat(balanceOf("a@test.com")).isEqualByComparingTo("30");
    }

    @Test
    void transfer_movesMoneyAtomicallyAndRecordsBothSides() throws Exception {
        String sender = registerAndLogin("a@test.com");
        String recipient = registerAndLogin("b@test.com");
        fund("a@test.com", "100.00");
        fund("b@test.com", "50.00");

        mockMvc.perform(money("/api/wallet/transfer", sender,
                        "{\"recipientEmail\":\"b@test.com\",\"amount\":70,\"description\":\"rent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.balanceBefore").value(100))
                .andExpect(jsonPath("$.balanceAfter").value(30));

        assertThat(balanceOf("a@test.com")).isEqualByComparingTo("30");
        assertThat(balanceOf("b@test.com")).isEqualByComparingTo("120");

        mockMvc.perform(get("/api/wallet/transactions").header("Authorization", recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER_IN"))
                .andExpect(jsonPath("$.content[0].balanceBefore").value(50))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(120));

        Integer sharedReferences = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT reference) FROM wallet_transactions", Integer.class);
        assertThat(sharedReferences).isEqualTo(1);
    }

    @Test
    void transfer_withInsufficientFunds_changesNothing() throws Exception {
        String sender = registerAndLogin("a@test.com");
        register("b@test.com");
        fund("a@test.com", "10.00");

        mockMvc.perform(money("/api/wallet/transfer", sender,
                        "{\"recipientEmail\":\"b@test.com\",\"amount\":10.01}"))
                .andExpect(status().isBadRequest());

        assertThat(balanceOf("a@test.com")).isEqualByComparingTo("10");
        assertThat(balanceOf("b@test.com")).isEqualByComparingTo("0");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wallet_transactions", Integer.class)).isZero();
    }

    @Test
    void transfer_toSelf_unknownOrInactiveRecipient_isRejected() throws Exception {
        String sender = registerAndLogin("a@test.com");
        register("b@test.com");
        fund("a@test.com", "100.00");

        mockMvc.perform(money("/api/wallet/transfer", sender, "{\"recipientEmail\":\"a@test.com\",\"amount\":5}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(money("/api/wallet/transfer", sender, "{\"recipientEmail\":\"nobody@test.com\",\"amount\":5}"))
                .andExpect(status().isNotFound());

        jdbcTemplate.update("UPDATE users SET active = FALSE WHERE email = 'b@test.com'");
        mockMvc.perform(money("/api/wallet/transfer", sender, "{\"recipientEmail\":\"b@test.com\",\"amount\":5}"))
                .andExpect(status().isConflict());

        assertThat(balanceOf("a@test.com")).isEqualByComparingTo("100");
    }

    @Test
    void idempotencyKey_replaysInsteadOfAppliedTwice() throws Exception {
        String auth = registerAndLogin("a@test.com");
        String body = "{\"amount\":25}";

        String first = mockMvc.perform(money("/api/wallet/deposit", auth, body).header("Idempotency-Key", "key-1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(money("/api/wallet/deposit", auth, body).header("Idempotency-Key", "key-1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(second).isEqualTo(first);
        assertThat(balanceOf("a@test.com")).isEqualByComparingTo("25");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wallet_transactions", Integer.class)).isEqualTo(1);
    }

    @Test
    void idempotencyKey_reusedForDifferentRequest_returns409() throws Exception {
        String auth = registerAndLogin("a@test.com");

        mockMvc.perform(money("/api/wallet/deposit", auth, "{\"amount\":25}").header("Idempotency-Key", "key-1"))
                .andExpect(status().isOk());
        mockMvc.perform(money("/api/wallet/deposit", auth, "{\"amount\":30}").header("Idempotency-Key", "key-1"))
                .andExpect(status().isConflict());

        assertThat(balanceOf("a@test.com")).isEqualByComparingTo("25");
    }

    @Test
    void transactionHistory_isPagedNewestFirst() throws Exception {
        String auth = registerAndLogin("a@test.com");
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(money("/api/wallet/deposit", auth, "{\"amount\":" + i + "}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/wallet/transactions?page=0&size=2").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].amount").value(3));

        mockMvc.perform(get("/api/wallet/transactions?page=1&size=2").header("Authorization", auth))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(1));
    }

    @Test
    void concurrentWithdrawals_cannotOverspend() throws Exception {
        register("a@test.com");
        fund("a@test.com", "100.00");
        User user = userRepository.findUserByEmail("a@test.com").orElseThrow();

        AtomicInteger succeeded = new AtomicInteger();
        runConcurrently(20, () -> {
            try {
                walletService.withdraw(user, new BigDecimal("10.00"), null, null);
                succeeded.incrementAndGet();
            } catch (RuntimeException expected) {
                // insufficient balance once the money is gone
            }
            return null;
        });

        assertThat(succeeded.get()).isEqualTo(10);
        assertThat(balanceOf("a@test.com")).isEqualByComparingTo("0");
    }

    @Test
    void concurrentOppositeTransfers_doNotDeadlockAndConserveMoney() throws Exception {
        register("a@test.com");
        register("b@test.com");
        fund("a@test.com", "500.00");
        fund("b@test.com", "500.00");
        User a = userRepository.findUserByEmail("a@test.com").orElseThrow();
        User b = userRepository.findUserByEmail("b@test.com").orElseThrow();

        AtomicInteger counter = new AtomicInteger();
        runConcurrently(20, () -> {
            boolean fromA = counter.getAndIncrement() % 2 == 0;
            walletService.transfer(fromA ? a : b, fromA ? "b@test.com" : "a@test.com",
                    new BigDecimal("5.00"), null, null);
            return null;
        });

        BigDecimal total = balanceOf("a@test.com").add(balanceOf("b@test.com"));
        assertThat(total).isEqualByComparingTo("1000");
    }

    private void runConcurrently(int threads, Callable<Void> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            }));
        }
        ready.await();
        go.countDown();
        for (Future<Void> future : futures) {
            future.get();
        }
        pool.shutdown();
    }
}
