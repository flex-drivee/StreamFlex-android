package com.streamflex.core.network

import android.content.Context
import com.streamflex.app.StreamFlexApplication
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress

enum class DohProvider(val title: String) {
    NONE("None (System DNS)"),
    GOOGLE("Google (8.8.8.8)"),
    CLOUDFLARE("Cloudflare (1.1.1.1)"),
    ADGUARD("AdGuard (No Ads)"),
    QUAD9("Quad9")
}

object DohProviders {

    private fun getSavedProvider(): DohProvider {
        return try {
            val prefs = StreamFlexApplication.instance.getSharedPreferences("streamflex_settings", Context.MODE_PRIVATE)
            val saved = prefs.getString("doh_provider", DohProvider.NONE.name) ?: DohProvider.NONE.name
            DohProvider.valueOf(saved)
        } catch (e: Exception) {
            DohProvider.NONE
        }
    }

    private fun OkHttpClient.Builder.addGenericDns(url: String, ips: List<String>) = dns(
        DnsOverHttps
            .Builder()
            .client(build())
            .url(url.toHttpUrl())
            .bootstrapDnsHosts(
                ips.map { InetAddress.getByName(it) }
            )
            .build()
    )

    fun applyDoh(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        return when (getSavedProvider()) {
            DohProvider.NONE -> builder
            DohProvider.GOOGLE -> builder.addGenericDns(
                "https://dns.google/dns-query",
                listOf("8.8.4.4", "8.8.8.8")
            )
            DohProvider.CLOUDFLARE -> builder.addGenericDns(
                "https://cloudflare-dns.com/dns-query",
                listOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001")
            )
            DohProvider.ADGUARD -> builder.addGenericDns(
                "https://dns.adguard.com/dns-query",
                listOf("94.140.14.14", "94.140.15.15")
            )
            DohProvider.QUAD9 -> builder.addGenericDns(
                "https://dns.quad9.net/dns-query",
                listOf("9.9.9.9", "149.112.112.112")
            )
        }
    }
}
