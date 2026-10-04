package com.app.wallet;

import com.app.wallet.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocsTest extends IntegrationTestBase {

    @Test
    void apiDocs_arePublicAndDescribeSecurityAndEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Digital Wallet API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/wallet/transfer'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/api/admin/users'].get.responses['403']").exists())
                .andExpect(jsonPath("$.components.schemas.ApiErrorDto").exists());
    }
}
