package com.cinetheta.core.network

import android.content.Context
import com.cinetheta.app.CineThetaApplication
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

enum class DohProvider(val title: String) {
    GOOGLE("Google (8.8.8.8)"),
    CLOUDFLARE("Cloudflare (1.1.1.1)"),
    ADGUARD("AdGuard (No Ads)"),
    QUAD9("Quad9"),
    NONE("None (System DNS)")
}

object DohProviders {

    fun getSavedProvider(): DohProvider {
        return try {
            val prefs = CineThetaApplication.instance.getSharedPreferences("cinetheta_settings", Context.MODE_PRIVATE)
            val saved = prefs.getString("doh_provider", DohProvider.GOOGLE.name) ?: DohProvider.GOOGLE.name
            DohProvider.valueOf(saved)
        } catch (e: Exception) {
            DohProvider.GOOGLE
        }
    }

    private val bootstrapClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(Dns.SYSTEM)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val googleDns: Dns by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://dns.google/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                listOf("8.8.4.4", "8.8.8.8").map { InetAddress.getByName(it) }
            )
            .build()
    }

    private val cloudflareDns: Dns by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                listOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001").map { InetAddress.getByName(it) }
            )
            .build()
    }

    private val adguardDns: Dns by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://dns.adguard.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                listOf("94.140.14.14", "94.140.15.15").map { InetAddress.getByName(it) }
            )
            .build()
    }

    private val quad9Dns: Dns by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://dns.quad9.net/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                listOf("9.9.9.9", "149.112.112.112").map { InetAddress.getByName(it) }
            )
            .build()
    }

    private fun getPrimaryDns(provider: DohProvider): Dns {
        return when (provider) {
            DohProvider.NONE -> Dns.SYSTEM
            DohProvider.GOOGLE -> googleDns
            DohProvider.CLOUDFLARE -> cloudflareDns
            DohProvider.ADGUARD -> adguardDns
            DohProvider.QUAD9 -> quad9Dns
        }
    }

    private val CLEAN_CLOUDFLARE_IPS by lazy {
        listOf(
            "104.21.33.54",
            "172.67.189.12",
            "104.21.21.42",
            "172.67.196.98",
            "104.16.132.229"
        ).mapNotNull {
            try { InetAddress.getByName(it) } catch (_: Exception) { null }
        }
    }

    private fun sanitizeAddresses(addresses: List<InetAddress>): List<InetAddress> {
        val result = mutableListOf<InetAddress>()
        var hasBlockedCloudflare = false

        for (addr in addresses) {
            val ipStr = addr.hostAddress ?: ""
            if (ipStr.startsWith("188.114.96.") || ipStr.startsWith("188.114.97.")) {
                hasBlockedCloudflare = true
            } else {
                result.add(addr)
            }
        }

        if (hasBlockedCloudflare) {
            result.addAll(0, CLEAN_CLOUDFLARE_IPS)
        }

        // Sort IPv4 first to avoid IPv6 route timeouts on mobile networks
        return result.sortedWith(compareBy { if (it is java.net.Inet4Address) 0 else 1 })
    }

    val dns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val currentProvider = getSavedProvider()
            val primary = getPrimaryDns(currentProvider)
            val fallback = if (currentProvider == DohProvider.NONE) googleDns else Dns.SYSTEM

            val rawAddresses = try {
                val addresses = primary.lookup(hostname)
                if (addresses.isNotEmpty()) addresses else fallback.lookup(hostname)
            } catch (primaryEx: Exception) {
                try {
                    fallback.lookup(hostname)
                } catch (fallbackEx: Exception) {
                    if (primaryEx is UnknownHostException) throw primaryEx
                    if (fallbackEx is UnknownHostException) throw fallbackEx
                    throw UnknownHostException("Unable to resolve host \"$hostname\": ${primaryEx.message}")
                }
            }

            return sanitizeAddresses(rawAddresses)
        }
    }

    fun applyDoh(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        return builder.dns(dns)
    }
}
