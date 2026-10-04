package com.app.wallet;

import com.app.wallet.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends IntegrationTestBase {

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    void validToken_returnsCurrentUser() throws Exception {
        String auth = registerAndLogin("a@test.com");

        mockMvc.perform(get("/api/users/me").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@test.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void garbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer    "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        register("a@test.com");
        String token = tokenFor(userId("a@test.com"), -1000);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has expired"));
    }

    @Test
    void tamperedSignature_returns401() throws Exception {
        register("a@test.com");
        String token = tokenFor(userId("a@test.com"), 60_000);
        String tampered = token.substring(0, token.length() - 3) + "abc";

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSubject_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenFor("not-a-number", 60_000)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedUser_returns401() throws Exception {
        String auth = registerAndLogin("a@test.com");
        jdbcTemplate.update("DELETE FROM wallets");
        jdbcTemplate.update("DELETE FROM users");

        mockMvc.perform(get("/api/users/me").header("Authorization", auth))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedUser_returns403() throws Exception {
        String auth = registerAndLogin("a@test.com");
        jdbcTemplate.update("UPDATE users SET active = FALSE");

        mockMvc.perform(get("/api/users/me").header("Authorization", auth))
                .andExpect(status().isForbidden());
    }

    @Test
    void staleTokenDoesNotBreakLogin() throws Exception {
        register("a@test.com");

        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@test.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void user_onAdminEndpoint_returns403() throws Exception {
        String auth = registerAndLogin("user@test.com");

        mockMvc.perform(get("/api/admin/users").header("Authorization", auth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void admin_onAdminEndpoint_returns200() throws Exception {
        String auth = registerAdminAndLogin("admin@test.com");

        mockMvc.perform(get("/api/admin/users").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("admin@test.com"));
    }

    @Test
    void anonymous_onAdminEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
