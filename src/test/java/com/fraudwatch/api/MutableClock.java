package com.fraudwatch.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock the tests control. Without it, results would depend on the real time:
 * a test run at 2 a.m. would trigger the night rule and fail.
 */
public class MutableClock extends Clock {

    /** 12:00 in Montreal: no night-time risk. */
    public static final Instant NOON_MONTREAL = Instant.parse("2026-01-15T17:00:00Z");

    private Instant now = NOON_MONTREAL;

    public synchronized void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public synchronized Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
