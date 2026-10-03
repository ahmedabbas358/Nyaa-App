package com.aniflow.domain.valueobject

import java.util.Locale

/**
 * Value Object representing a byte quantity with invariant validation.
 * Invariant: bytes cannot be negative (Section 97 & 98).
 * Supports standard arithmetic, comparison, and human-friendly string formatting.
 */
@JvmInline
value class ByteSize(val bytes: Long) : Comparable<ByteSize> {

    init {
        require(bytes >= 0L) { "ByteSize cannot be negative: $bytes" }
    }

    operator fun plus(other: ByteSize): ByteSize = ByteSize(this.bytes + other.bytes)

    operator fun minus(other: ByteSize): ByteSize = ByteSize((this.bytes - other.bytes).coerceAtLeast(0L))

    operator fun times(scalar: Int): ByteSize {
        require(scalar >= 0) { "Scalar cannot be negative: $scalar" }
        return ByteSize(this.bytes * scalar)
    }

    operator fun div(divisor: Long): ByteSize {
        require(divisor > 0) { "Divisor must be greater than zero" }
        return ByteSize(this.bytes / divisor)
    }

    override fun compareTo(other: ByteSize): Int = this.bytes.compareTo(other.bytes)

    val inKilobytes: Double get() = bytes / 1024.0
    val inMegabytes: Double get() = bytes / (1024.0 * 1024.0)
    val inGigabytes: Double get() = bytes / (1024.0 * 1024.0 * 1024.0)
    val inTerabytes: Double get() = bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0)

    val formatted: String get() = toDisplayString()

    fun toDisplayString(): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.2f %s", value, units[digitGroups])
    }

    override fun toString(): String = toDisplayString()

    companion object {
        val ZERO = ByteSize(0L)

        fun ofBytes(bytes: Long): ByteSize = ByteSize(bytes.coerceAtLeast(0L))
        fun fromBytes(bytes: Long): ByteSize = ofBytes(bytes)

        fun ofKilobytes(kb: Double): ByteSize {
            require(kb >= 0.0) { "Kilobytes cannot be negative" }
            return ByteSize((kb * 1024.0).toLong())
        }
        fun fromKilobytes(kb: Double): ByteSize = ofKilobytes(kb)

        fun ofMegabytes(mb: Double): ByteSize {
            require(mb >= 0.0) { "Megabytes cannot be negative" }
            return ByteSize((mb * 1024.0 * 1024.0).toLong())
        }
        fun fromMegabytes(mb: Double): ByteSize = ofMegabytes(mb)

        fun ofGigabytes(gb: Double): ByteSize {
            require(gb >= 0.0) { "Gigabytes cannot be negative" }
            return ByteSize((gb * 1024.0 * 1024.0 * 1024.0).toLong())
        }
        fun fromGigabytes(gb: Double): ByteSize = ofGigabytes(gb)
    }
}

typealias FileSize = ByteSize
