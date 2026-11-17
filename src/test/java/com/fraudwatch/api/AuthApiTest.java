package com.fraudwatch.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AuthApiTest extends ApiTestSupport {

    private ResultActions postJson(String url, Map<String, String> body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
    }

    @Test
    void registersACustomer() throws Exception {
        String username = uniqueUsername();

        postJson("/api/auth/register", Map.of("username", username, "password", "Password123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("USER"))
                // the password hash must never be returned
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void refusesDuplicateUsername() throws Exception {
        String username = uniqueUsername();
        register(username, "Password123");

        postJson("/api/auth/register", Map.of("username", username, "password", "Password123"))
                .andExpect(status().isConflict());
    }

    @Test
    void refusesWeakPassword() throws Exception {
        postJson("/api/auth/register", Map.of("username", uniqueUsername(), "password", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    void refusesInvalidUsernameCharacters() throws Exception {
        postJson("/api/auth/register", Map.of("username", "<script>", "password", "Password123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username", notNullValue()));
    }

    @Test
    void returnsATokenOnValidLogin() throws Exception {
        String username = uniqueUsername();
        register(username, "Password123");

        postJson("/api/auth/login", Map.of("username", username, "password", "Password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void sameErrorForWrongPasswordAndUnknownUser() throws Exception {
        String username = uniqueUsername();
        register(username, "Password123");

        postJson("/api/auth/login", Map.of("username", username, "password", "WrongPass1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
        postJson("/api/auth/login", Map.of("username", uniqueUsername(), "password", "WrongPass1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void locksTheUsernameAfterFiveFailures() throws Exception {
        String username = uniqueUsername();
        register(username, "Password123");

        for (int i = 0; i < 5; i++) {
            postJson("/api/auth/login", Map.of("username", username, "password", "WrongPass1"))
                    .andExpect(status().isUnauthorized());
        }

        // Even the right password is refused while locked
        postJson("/api/auth/login", Map.of("username", username, "password", "Password123"))
                .andExpect(status().isLocked())
                .andExpect(header().string("Retry-After", notNullValue()))
                .andExpect(jsonPath("$.detail", containsString("Too many failed login attempts")));
    }
}
