package com.aniflow.core.logging

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    CRITICAL
}

interface Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
    fun critical(tag: String, message: String, throwable: Throwable? = null)
}

object SystemLogger : Logger {
    override fun debug(tag: String, message: String) {
        println("[DEBUG][$tag] $message")
    }

    override fun info(tag: String, message: String) {
        println("[INFO][$tag] $message")
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        println("[WARN][$tag] $message")
        throwable?.printStackTrace()
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        System.err.println("[ERROR][$tag] $message")
        throwable?.printStackTrace()
    }

    override fun critical(tag: String, message: String, throwable: Throwable?) {
        System.err.println("[CRITICAL][$tag] $message")
        throwable?.printStackTrace()
    }
}
