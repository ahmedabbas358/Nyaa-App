package com.aniflow.core.network.resilience

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resilient Multi-Tier DNS Resolver for Nyaa.si and Asian media trackers.
 * Designed specifically to bypass ISP-level DNS censorship, DNS poisoning,
 * and regional blockades (e.g. in MENA/Egypt/Global) using:
 *
 * 1. Cloudflare DNS-over-HTTPS (DoH) API over direct IP endpoints (1.1.1.1 / 1.0.0.1)
 * 2. Google DNS-over-HTTPS (DoH) API over direct IP endpoints (8.8.8.8 / 8.8.4.4)
 * 3. Quad9 DNS-over-HTTPS (DoH) API over direct IP endpoint (9.9.9.9)
 * 4. Rigorous IP filtering to eliminate ISP poisoned responses (127.0.0.1, 0.0.0.0, private subnets)
 * 5. Hostname-preserving InetAddress construction for proper TLS SNI & certificate verification
 * 6. Hardcoded DDoS-Guard verified reverse-proxy IP fallback for nyaa.si
 */
class ResilientNyaaDns(
    private val dohTimeoutMs: Long = 4000L
) : Dns {

    private val cache = ConcurrentHashMap<String, List<InetAddress>>()
    private val ipRegex = Regex(""""data"\s*:\s*"([0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3})"""")

    // Standalone OkHttpClient for DoH queries to prevent recursive DNS lookups
    private val dohClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(dohTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(dohTimeoutMs, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val cached = cache[hostname]
        if (!cached.isNullOrEmpty()) {
            return cached
        }

        val isNyaaDomain = hostname.contains("nyaa", ignoreCase = true)

        // For Nyaa domains: Prioritize DoH FIRST to prevent ISP poisoned DNS (which returns 127.0.0.1)
        if (isNyaaDomain) {
            val dohResult = resolveViaDoH(hostname)
            if (dohResult.isNotEmpty()) {
                cache[hostname] = dohResult
                return dohResult
            }

            // Fallback: Try System DNS only if it returns clean, non-poisoned public IPs
            try {
                val systemAddresses = Dns.SYSTEM.lookup(hostname)
                val cleanSystemAddresses = systemAddresses.filter { addr ->
                    !addr.isLoopbackAddress && !addr.isAnyLocalAddress && !addr.isSiteLocalAddress &&
                            isValidPublicIp(addr.hostAddress ?: "")
                }
                if (cleanSystemAddresses.isNotEmpty()) {
                    cache[hostname] = cleanSystemAddresses
                    return cleanSystemAddresses
                }
            } catch (_: Exception) {}

            // Ultimate Fallback: Active DDoS-Guard reverse proxy IP addresses for nyaa.si
            if (hostname.equals("nyaa.si", ignoreCase = true) || hostname.equals("www.nyaa.si", ignoreCase = true)) {
                val fallbackAddresses = listOfNotNull(
                    createInetAddress(hostname, "186.2.163.20"),
                    createInetAddress(hostname, "129.222.68.133"),
                    createInetAddress(hostname, "185.178.208.181")
                )
                if (fallbackAddresses.isNotEmpty()) {
                    cache[hostname] = fallbackAddresses
                    return fallbackAddresses
                }
            }

            throw UnknownHostException("Unable to resolve Nyaa host '$hostname' via DoH or System DNS")
        }

        // For non-Nyaa domains (e.g. Anilist, GitHub, TMDB): Try System DNS first
        try {
            val addresses = Dns.SYSTEM.lookup(hostname)
            if (addresses.isNotEmpty()) {
                cache[hostname] = addresses
                return addresses
            }
        } catch (_: Exception) {}

        // Fallback to DoH for non-Nyaa domains if system DNS failed
        val dohResult = resolveViaDoH(hostname)
        if (dohResult.isNotEmpty()) {
            cache[hostname] = dohResult
            return dohResult
        }

        throw UnknownHostException("Unable to resolve host '$hostname' via System or DoH")
    }

    private fun resolveViaDoH(hostname: String): List<InetAddress> {
        val dohEndpoints = listOf(
            DoHEndpoint("https://1.1.1.1/dns-query?name=$hostname&type=A", isJsonAccept = true),
            DoHEndpoint("https://1.0.0.1/dns-query?name=$hostname&type=A", isJsonAccept = true),
            DoHEndpoint("https://8.8.8.8/resolve?name=$hostname&type=A", isJsonAccept = false),
            DoHEndpoint("https://8.8.4.4/resolve?name=$hostname&type=A", isJsonAccept = false),
            DoHEndpoint("https://9.9.9.9/dns-query?name=$hostname&type=A", isJsonAccept = true)
        )

        for (endpoint in dohEndpoints) {
            try {
                val reqBuilder = Request.Builder().url(endpoint.url)
                if (endpoint.isJsonAccept) {
                    reqBuilder.header("Accept", "application/dns-json")
                }
                dohClient.newCall(reqBuilder.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val ipStrings = ipRegex.findAll(body)
                            .map { it.groupValues[1] }
                            .filter { isValidPublicIp(it) }
                            .distinct()
                            .toList()

                        if (ipStrings.isNotEmpty()) {
                            val resolvedAddresses = ipStrings.mapNotNull { ip ->
                                createInetAddress(hostname, ip)
                            }
                            if (resolvedAddresses.isNotEmpty()) {
                                return resolvedAddresses
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Continue to next DoH provider
            }
        }

        return emptyList()
    }

    private fun createInetAddress(hostname: String, ipString: String): InetAddress? {
        val parts = ipString.split(".")
        if (parts.size != 4) return null
        val bytes = ByteArray(4)
        for (i in 0..3) {
            val num = parts[i].toIntOrNull() ?: return null
            if (num !in 0..255) return null
            bytes[i] = num.toByte()
        }
        return try {
            InetAddress.getByAddress(hostname, bytes)
        } catch (_: Exception) {
            null
        }
    }

    private fun isValidPublicIp(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        val nums = parts.map { it.toIntOrNull() ?: return false }
        if (nums.any { it !in 0..255 }) return false

        val first = nums[0]
        val second = nums[1]

        // Reject 0.0.0.0 and Loopback 127.0.0.0/8
        if (first == 0 || first == 127) return false

        // Reject Private subnets
        if (first == 10) return false // 10.0.0.0/8
        if (first == 172 && second in 16..31) return false // 172.16.0.0/12
        if (first == 192 && second == 168) return false // 192.168.0.0/16

        // Reject Link-Local 169.254.0.0/16
        if (first == 169 && second == 254) return false

        return true
    }

    private data class DoHEndpoint(
        val url: String,
        val isJsonAccept: Boolean
    )
}
