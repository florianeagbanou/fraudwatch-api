package com.fraudwatch.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base class for the API tests: starts the whole application (H2 database included)
 * and offers small helpers so each test reads like a scenario.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiTestSupport.TestClockConfig.class)
abstract class ApiTestSupport {

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected Clock clock;

    protected MutableClock testClock() {
        return (MutableClock) clock;
    }

    /** Unique usernames: tests share one database and must not step on each other. */
    protected static String uniqueUsername() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String toJson(Object body) throws Exception {
        return json.writeValueAsString(body);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected void register(String username, String password) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", username, "password", password))))
                .andExpect(status().isCreated());
    }

    protected String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).get("accessToken").asText();
    }

    /** Registers a brand new customer and returns their token. */
    protected String newCustomer() throws Exception {
        String username = uniqueUsername();
        register(username, "Password123");
        return login(username, "Password123");
    }

    protected String analystToken() throws Exception {
        return login("analyst", "Analyst123!");
    }

    protected String adminToken() throws Exception {
        return login("admin", "Admin123!");
    }

    protected long openAccount(String token) throws Exception {
        MvcResult result = mvc.perform(withToken(post("/api/accounts"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("homeCountry", "CA", "currency", "CAD"))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).get("id").asLong();
    }

    protected JsonNode transaction(String token, long accountId, String type, String amount, String country)
            throws Exception {
        MvcResult result = mvc.perform(withToken(post("/api/accounts/" + accountId + "/transactions"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("type", type, "amount", amount, "country", country))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result);
    }

    protected JsonNode account(String token, long accountId) throws Exception {
        return read(mvc.perform(withToken(get("/api/accounts/" + accountId), token))
                .andExpect(status().isOk()).andReturn());
    }

    /** Finds the alert opened for a given transaction. */
    protected long alertIdFor(long transactionId) throws Exception {
        JsonNode alerts = read(mvc.perform(withToken(get("/api/alerts?status=OPEN"), analystToken()))
                .andExpect(status().isOk()).andReturn());
        for (JsonNode alert : alerts) {
            if (alert.get("transactionId").asLong() == transactionId) {
                return alert.get("id").asLong();
            }
        }
        throw new AssertionError("No open alert for transaction " + transactionId);
    }
}
