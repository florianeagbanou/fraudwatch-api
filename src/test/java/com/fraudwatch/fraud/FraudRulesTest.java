package com.fraudwatch.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import com.fraudwatch.fraud.rules.ForeignCountryRule;
import com.fraudwatch.fraud.rules.ImpossibleTravelRule;
import com.fraudwatch.fraud.rules.LargeAmountRule;
import com.fraudwatch.fraud.rules.NightActivityRule;
import com.fraudwatch.fraud.rules.UnusualAmountRule;
import com.fraudwatch.fraud.rules.VelocityRule;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** One nested class per rule. Each test describes one business case in its name. */
class FraudRulesTest {

    private static final FraudSettings SETTINGS = FraudSettings.defaults();
    /** 12:00 in Montreal (17:00 UTC in winter): daytime, so the night rule stays quiet. */
    private static final Instant NOON = Instant.parse("2026-01-15T17:00:00Z");

    private static TransactionContext tx(String amount, String country, List<PastTransaction> history) {
        return new TransactionContext(new BigDecimal(amount), country, "CA", NOON, history);
    }

    private static PastTransaction past(String amount, String country, Duration ago) {
        return new PastTransaction(new BigDecimal(amount), country, NOON.minus(ago), false);
    }

    @Nested
    class LargeAmount {
        private final LargeAmountRule rule = new LargeAmountRule(SETTINGS);

        @Test
        void matchesAtTheThreshold() {
            assertThat(rule.evaluate(tx("10000", "CA", List.of()))).isPresent();
        }

        @Test
        void ignoresSmallerAmounts() {
            assertThat(rule.evaluate(tx("9999.99", "CA", List.of()))).isEmpty();
        }
    }

    @Nested
    class UnusualAmount {
        private final UnusualAmountRule rule = new UnusualAmountRule(SETTINGS);
        private final List<PastTransaction> habits = List.of(
                past("100", "CA", Duration.ofDays(1)),
                past("100", "CA", Duration.ofDays(2)),
                past("100", "CA", Duration.ofDays(3)));

        @Test
        void matchesWhenFarAboveTheAverage() {
            assertThat(rule.evaluate(tx("501", "CA", habits))).isPresent();
        }

        @Test
        void ignoresExactlyFiveTimesTheAverage() {
            assertThat(rule.evaluate(tx("500", "CA", habits))).isEmpty();
        }

        @Test
        void needsEnoughHistoryToJudge() {
            assertThat(rule.evaluate(tx("5000", "CA", habits.subList(0, 2)))).isEmpty();
        }

        @Test
        void ignoresBlockedTransactionsInTheAverage() {
            List<PastTransaction> withBlocked = new ArrayList<>(habits);
            withBlocked.add(new PastTransaction(new BigDecimal("100000"), "CA", NOON.minus(Duration.ofDays(4)), true));

            assertThat(rule.evaluate(tx("501", "CA", withBlocked))).isPresent();
        }
    }

    @Nested
    class Velocity {
        private final VelocityRule rule = new VelocityRule(SETTINGS);

        private List<PastTransaction> recent(int count) {
            List<PastTransaction> history = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                history.add(past("10", "CA", Duration.ofMinutes(i)));
            }
            return history;
        }

        @Test
        void matchesTheSixthTransactionInTenMinutes() {
            assertThat(rule.evaluate(tx("10", "CA", recent(5)))).isPresent();
        }

        @Test
        void ignoresTheFifth() {
            assertThat(rule.evaluate(tx("10", "CA", recent(4)))).isEmpty();
        }

        @Test
        void ignoresOlderTransactions() {
            List<PastTransaction> old = List.of(
                    past("10", "CA", Duration.ofMinutes(11)), past("10", "CA", Duration.ofMinutes(12)),
                    past("10", "CA", Duration.ofMinutes(13)), past("10", "CA", Duration.ofMinutes(14)),
                    past("10", "CA", Duration.ofMinutes(15)));
            assertThat(rule.evaluate(tx("10", "CA", old))).isEmpty();
        }
    }

    @Nested
    class ForeignCountry {
        private final ForeignCountryRule rule = new ForeignCountryRule();

        @Test
        void matchesOutsideHomeCountry() {
            assertThat(rule.evaluate(tx("10", "FR", List.of()))).isPresent();
        }

        @Test
        void ignoresHomeCountry() {
            assertThat(rule.evaluate(tx("10", "CA", List.of()))).isEmpty();
        }
    }

    @Nested
    class ImpossibleTravel {
        private final ImpossibleTravelRule rule = new ImpossibleTravelRule(SETTINGS);

        @Test
        void matchesTwoCountriesWithinTwoHours() {
            assertThat(rule.evaluate(tx("10", "FR", List.of(past("10", "CA", Duration.ofMinutes(30)))))).isPresent();
        }

        @Test
        void ignoresTwoCountriesFarApartInTime() {
            assertThat(rule.evaluate(tx("10", "FR", List.of(past("10", "CA", Duration.ofHours(3)))))).isEmpty();
        }

        @Test
        void ignoresSameCountry() {
            assertThat(rule.evaluate(tx("10", "CA", List.of(past("10", "CA", Duration.ofMinutes(5)))))).isEmpty();
        }

        @Test
        void ignoresFirstTransaction() {
            assertThat(rule.evaluate(tx("10", "FR", List.of()))).isEmpty();
        }
    }

    @Nested
    class NightActivity {
        private final NightActivityRule rule = new NightActivityRule(SETTINGS);

        @ParameterizedTest(name = "{0} UTC -> match: {1}")
        @CsvSource({
                // Montreal is UTC-5 in January
                "2026-01-15T05:00:00Z, true",   // 00:00 local
                "2026-01-15T09:59:00Z, true",   // 04:59 local
                "2026-01-15T10:00:00Z, false",  // 05:00 local
                "2026-01-15T17:00:00Z, false"   // 12:00 local
        })
        void matchesBetweenMidnightAndFiveLocalTime(String utc, boolean expected) {
            TransactionContext context = new TransactionContext(BigDecimal.TEN, "CA", "CA", Instant.parse(utc), List.of());
            assertThat(rule.evaluate(context).isPresent()).isEqualTo(expected);
        }
    }
}
