package com.fraudwatch.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** End-to-end business scenarios through the real HTTP API and database. */
class TransactionFlowApiTest extends ApiTestSupport {

    private String customer;
    private long accountId;

    @BeforeEach
    void setUp() throws Exception {
        // Move time forward so earlier tests' transactions never count as "recent"
        testClock().advance(Duration.ofDays(1));
        customer = newCustomer();
        accountId = openAccount(customer);
    }

    private static List<String> ruleCodes(JsonNode tx) {
        List<String> codes = new ArrayList<>();
        tx.get("triggeredRules").forEach(rule -> codes.add(rule.get("code").asText()));
        return codes;
    }

    private void review(long alertId, String resolution) throws Exception {
        mvc.perform(withToken(patch("/api/alerts/" + alertId), analystToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("resolution", resolution, "note", "checked"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(resolution));
    }

    @Test
    void normalDepositIsApproved() throws Exception {
        JsonNode tx = transaction(customer, accountId, "DEPOSIT", "1000.00", "CA");

        assertThat(tx.get("status").asText()).isEqualTo("APPROVED");
        assertThat(tx.get("riskScore").asInt()).isZero();
        assertThat(tx.get("balanceAfter").decimalValue()).isEqualByComparingTo("1000.00");
    }

    @Test
    void paymentAbroadRightAfterAHomePaymentIsFlagged() throws Exception {
        transaction(customer, accountId, "DEPOSIT", "1000.00", "CA");

        JsonNode tx = transaction(customer, accountId, "PAYMENT", "50.00", "FR");

        assertThat(tx.get("status").asText()).isEqualTo("FLAGGED");
        assertThat(tx.get("riskScore").asInt()).isEqualTo(60);
        assertThat(ruleCodes(tx)).containsExactlyInAnyOrder("FOREIGN_COUNTRY", "IMPOSSIBLE_TRAVEL");
        // flagged: the money already moved
        assertThat(tx.get("balanceAfter").decimalValue()).isEqualByComparingTo("950.00");
    }

    @Test
    void confirmedFraudReversesAFlaggedPayment() throws Exception {
        transaction(customer, accountId, "DEPOSIT", "1000.00", "CA");
        JsonNode tx = transaction(customer, accountId, "PAYMENT", "50.00", "FR");

        review(alertIdFor(tx.get("id").asLong()), "CONFIRMED_FRAUD");

        assertThat(account(customer, accountId).get("balance").decimalValue()).isEqualByComparingTo("1000.00");
    }

    @Test
    void highRiskWithdrawalIsBlockedThenReleasedAsFalsePositive() throws Exception {
        JsonNode deposit = transaction(customer, accountId, "DEPOSIT", "20000.00", "CA");
        assertThat(deposit.get("status").asText()).isEqualTo("FLAGGED"); // large amount alone: 40 points

        // large amount (40) + foreign (15) + impossible travel (45) = 100
        JsonNode withdrawal = transaction(customer, accountId, "WITHDRAWAL", "15000.00", "US");
        assertThat(withdrawal.get("status").asText()).isEqualTo("BLOCKED");
        assertThat(withdrawal.get("riskScore").asInt()).isEqualTo(100);
        assertThat(withdrawal.get("balanceAfter").decimalValue()).isEqualByComparingTo("20000.00");

        review(alertIdFor(withdrawal.get("id").asLong()), "FALSE_POSITIVE");

        // released: the money moves now
        assertThat(account(customer, accountId).get("balance").decimalValue()).isEqualByComparingTo("5000.00");
    }

    @Test
    void alertCannotBeReviewedTwice() throws Exception {
        transaction(customer, accountId, "DEPOSIT", "1000.00", "CA");
        JsonNode tx = transaction(customer, accountId, "PAYMENT", "50.00", "FR");
        long alertId = alertIdFor(tx.get("id").asLong());
        review(alertId, "FALSE_POSITIVE");

        mvc.perform(withToken(patch("/api/alerts/" + alertId), analystToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("resolution", "CONFIRMED_FRAUD"))))
                .andExpect(status().isConflict());
    }

    @Test
    void burstOfTransactionsTriggersVelocityRule() throws Exception {
        for (int i = 0; i < 5; i++) {
            transaction(customer, accountId, "DEPOSIT", "10.00", "CA");
            testClock().advance(Duration.ofMinutes(1));
        }

        JsonNode sixth = transaction(customer, accountId, "DEPOSIT", "10.00", "CA");

        assertThat(ruleCodes(sixth)).contains("VELOCITY");
    }

    @Test
    void refusesPaymentLargerThanBalance() throws Exception {
        transaction(customer, accountId, "DEPOSIT", "100.00", "CA");

        mvc.perform(withToken(post("/api/accounts/" + accountId + "/transactions"), customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("type", "PAYMENT", "amount", "100.01", "country", "CA"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Insufficient funds"));
    }

    @Test
    void validatesTransactionInput() throws Exception {
        mvc.perform(withToken(post("/api/accounts/" + accountId + "/transactions"), customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("type", "PAYMENT", "amount", "-5", "country", "canada"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.country").exists());
    }

    @Test
    void rejectsUnknownTransactionType() throws Exception {
        mvc.perform(withToken(post("/api/accounts/" + accountId + "/transactions"), customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("type", "STEAL", "amount", "5", "country", "CA"))))
                .andExpect(status().isBadRequest());
    }
}
