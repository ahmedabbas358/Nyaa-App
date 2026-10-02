package com.aniflow.core.common.time

import java.time.Instant

/**
 * Clock abstraction to eliminate unmanaged System.currentTimeMillis() or Instant.now()
 * calls across Domain, Data, and Provider modules, ensuring deterministic testing.
 */
interface Clock {
    fun now(): Instant
    fun currentTimeMillis(): Long = now().toEpochMilli()
}

/**
 * Production system clock.
 */
class SystemClock : Clock {
    override fun now(): Instant = Instant.now()
}

/**
 * Controllable clock for unit and integration testing.
 */
class FakeClock(
    private var currentInstant: Instant = Instant.ofEpochMilli(1_700_000_000_000L)
) : Clock {

    override fun now(): Instant = currentInstant

    fun setTime(instant: Instant) {
        currentInstant = instant
    }

    fun advanceBySeconds(seconds: Long) {
        currentInstant = currentInstant.plusSeconds(seconds)
    }

    fun advanceByMillis(millis: Long) {
        currentInstant = currentInstant.plusMillis(millis)
    }
}
