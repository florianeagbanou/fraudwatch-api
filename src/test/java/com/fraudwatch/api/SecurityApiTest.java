package com.fraudwatch.api;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** Who can access what. Each test tries to do something it should not be allowed to do. */
class SecurityApiTest extends ApiTestSupport {

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsForgedToken() throws Exception {
        String forged = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIkFETUlOIl19.invalidsignature";
        mvc.perform(withToken(get("/api/audit"), forged)).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotReadAlerts() throws Exception {
        mvc.perform(withToken(get("/api/alerts"), newCustomer())).andExpect(status().isForbidden());
    }

    @Test
    void customerCannotReadAuditLog() throws Exception {
        mvc.perform(withToken(get("/api/audit"), newCustomer())).andExpect(status().isForbidden());
    }

    @Test
    void analystCannotReadAuditLog() throws Exception {
        mvc.perform(withToken(get("/api/audit"), analystToken())).andExpect(status().isForbidden());
    }

    @Test
    void customerCannotSeeSomeoneElsesAccount() throws Exception {
        String alice = newCustomer();
        String bob = newCustomer();
        long aliceAccount = openAccount(alice);

        // 404, not 403: Bob must not even learn that the account exists
        mvc.perform(withToken(get("/api/accounts/" + aliceAccount), bob)).andExpect(status().isNotFound());
        mvc.perform(withToken(get("/api/accounts/" + aliceAccount + "/transactions"), bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminReadsAuditLog() throws Exception {
        newCustomer();
        mvc.perform(withToken(get("/api/audit?limit=10"), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    @Test
    void healthAndDocsArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
