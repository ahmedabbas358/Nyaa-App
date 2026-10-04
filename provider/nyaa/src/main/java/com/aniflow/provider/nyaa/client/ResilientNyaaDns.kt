package com.aniflow.provider.nyaa.client

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resilient Multi-Tier DNS Resolver for Nyaa.si.
 * Bypasses ISP-level DNS blocking and censorship (e.g. in MENA/Egypt/Global) using:
 * 1. Standard System DNS
 * 2. Cloudflare DNS-over-HTTPS (DoH) API (1.1.1.1)
 * 3. Google DNS-over-HTTPS (DoH) API (dns.google)
 * 4. Verified Server IP Fallbacks (186.2.163.20, 129.222.68.133)
 */
class ResilientNyaaDns(
    private val dohTimeoutMs: Long = 3500L
) : Dns {

    private val cache = ConcurrentHashMap<String, List<InetAddress>>()
    private val ipRegex = Regex(""""data"\s*:\s*"([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)"""")

    // Standalone OkHttpClient for DoH queries to prevent recursive DNS lookups
    private val dohClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(dohTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(dohTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val cached = cache[hostname]
        if (!cached.isNullOrEmpty()) {
            return cached
        }

        // 1. Try standard system DNS first
        try {
            val addresses = Dns.SYSTEM.lookup(hostname)
            if (addresses.isNotEmpty()) {
                cache[hostname] = addresses
                return addresses
            }
        } catch (_: Exception) {
            // System DNS failed / blocked by ISP
        }

        // 2. If it's a nyaa domain or mirror, resolve via DoH
        if (hostname.contains("nyaa", ignoreCase = true)) {
            // Query Cloudflare DoH (1.1.1.1)
            try {
                val cfAddresses = queryDoH("https://1.1.1.1/dns-query?name=$hostname&type=A", hostname, isCloudflare = true)
                if (cfAddresses.isNotEmpty()) {
                    cache[hostname] = cfAddresses
                    return cfAddresses
                }
            } catch (_: Exception) {}

            // Query Google DoH (dns.google)
            try {
                val googleAddresses = queryDoH("https://dns.google/resolve?name=$hostname&type=A", hostname, isCloudflare = false)
                if (googleAddresses.isNotEmpty()) {
                    cache[hostname] = googleAddresses
                    return googleAddresses
                }
            } catch (_: Exception) {}

            // 3. Fallback to verified active DDoS-Guard IP addresses for nyaa.si
            if (hostname.equals("nyaa.si", ignoreCase = true)) {
                val fallbackAddresses = listOf(
                    InetAddress.getByAddress(hostname, byteArrayOf(186.toByte(), 2.toByte(), 163.toByte(), 20.toByte())),
                    InetAddress.getByAddress(hostname, byteArrayOf(129.toByte(), 222.toByte(), 68.toByte(), 133.toByte()))
                )
                cache[hostname] = fallbackAddresses
                return fallbackAddresses
            }
        }

        throw UnknownHostException("Unable to resolve host $hostname via System or DoH")
    }

    private fun queryDoH(url: String, hostname: String, isCloudflare: Boolean): List<InetAddress> {
        val reqBuilder = Request.Builder().url(url)
        if (isCloudflare) {
            reqBuilder.header("Accept", "application/dns-json")
        }

        dohClient.newCall(reqBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string().orEmpty()
            val matches = ipRegex.findAll(body).map { it.groupValues[1] }.toList()
            if (matches.isEmpty()) return emptyList()

            return matches.mapNotNull { ip ->
                try {
                    InetAddress.getByName(ip)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}
