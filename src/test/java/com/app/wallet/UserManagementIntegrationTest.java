package com.app.wallet;

import com.app.wallet.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserManagementIntegrationTest extends IntegrationTestBase {

    private static final String PROFILE = "{\"firstName\":\"New\",\"lastName\":\"Name\",\"email\":\"%s\"}";

    @Test
    void updateMe_changesProfile() throws Exception {
        String auth = registerAndLogin("a@test.com");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFILE.formatted("new@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.email").value("new@test.com"));
    }

    @Test
    void updateMe_withTakenEmail_returns409() throws Exception {
        register("taken@test.com");
        String auth = registerAndLogin("a@test.com");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFILE.formatted("taken@test.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void updateMe_withInvalidBody_returns400WithFieldErrors() throws Exception {
        String auth = registerAndLogin("a@test.com");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"x\",\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void changePassword_thenOldPasswordStopsWorking() throws Exception {
        String auth = registerAndLogin("a@test.com");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"brandnew123\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@test.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@test.com\",\"password\":\"brandnew123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_withWrongCurrentPassword_returns400() throws Exception {
        String auth = registerAndLogin("a@test.com");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrongwrong\",\"newPassword\":\"brandnew123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_canGetAndUpdateUser() throws Exception {
        String admin = registerAdminAndLogin("admin@test.com");
        register("u@test.com");
        long id = userId("u@test.com");

        mockMvc.perform(get("/api/admin/users/" + id).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@test.com"));

        mockMvc.perform(put("/api/admin/users/" + id)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFILE.formatted("changed@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("changed@test.com"));
    }

    @Test
    void admin_getUnknownUser_returns404() throws Exception {
        String admin = registerAdminAndLogin("admin@test.com");

        mockMvc.perform(get("/api/admin/users/999999").header("Authorization", admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void admin_canChangeRole_andNewAdminGainsAccess() throws Exception {
        String admin = registerAdminAndLogin("admin@test.com");
        String user = registerAndLogin("u@test.com");
        long id = userId("u@test.com");

        mockMvc.perform(get("/api/admin/users").header("Authorization", user))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/" + id + "/role")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(get("/api/admin/users").header("Authorization", user))
                .andExpect(status().isOk());
    }

    @Test
    void admin_changeRoleToUnknownValue_returns400() throws Exception {
        String admin = registerAdminAndLogin("admin@test.com");
        register("u@test.com");

        mockMvc.perform(patch("/api/admin/users/" + userId("u@test.com") + "/role")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPERUSER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_deactivatedUserCannotLoginOrUseToken() throws Exception {
        String admin = registerAdminAndLogin("admin@test.com");
        String user = registerAndLogin("u@test.com");
        long id = userId("u@test.com");

        mockMvc.perform(patch("/api/admin/users/" + id + "/status")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/users/me").header("Authorization", user))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"u@test.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/" + id + "/status")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me").header("Authorization", user))
                .andExpect(status().isOk());
    }

    @Test
    void admin_cannotModifyOwnRoleStatusOrAccount() throws Exception {
        String admin = registerAdminAndLogin("admin@test.com");
        long id = userId("admin@test.com");

        mockMvc.perform(patch("/api/admin/users/" + id + "/role")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/admin/users/" + id + "/status")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/admin/users/" + id).header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_canDeleteUserWithoutActivity() throws Exception {
        String admin = registerAdminAndLogin("admin@test.com");
        register("u@test.com");
        long id = userId("u@test.com");

        mockMvc.perform(delete("/api/admin/users/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/users/" + id).header("Authorization", admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void admin_cannotDeleteUserWithBalance() throws Exception {
        String admin = registerAdminAndLogin("admin@test.com");
        register("u@test.com");
        long id = userId("u@test.com");
        jdbcTemplate.update("UPDATE wallets SET balance = 10 WHERE user_id = ?", id);

        mockMvc.perform(delete("/api/admin/users/" + id).header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    void regularUser_cannotUseAdminEndpoints() throws Exception {
        String user = registerAndLogin("u@test.com");
        long id = userId("u@test.com");

        mockMvc.perform(get("/api/admin/users/" + id).header("Authorization", user))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/users/" + id).header("Authorization", user))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/users/" + id + "/role")
                        .header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }
}
