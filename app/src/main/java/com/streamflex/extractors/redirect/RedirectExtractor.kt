package com.streamflex.extractors.redirect

import com.streamflex.core.network.detector.HostDetector
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper
import com.streamflex.extractors.shared.ExtractorUtils

/**
 * Generic redirect resolver.
 *
 * Handles pages such as:
 *
 * • gamerxyt
 * • go.php
 * • download.php
 * • redirect.php
 * • generic HTML gateways
 *
 * It never resolves streams itself.
 * It only discovers the next URLs and lets
 * ExtractorManager continue the chain.
 */
class RedirectExtractor : BaseExtractor() {

    override val hostType = HostType.REDIRECT

    override suspend fun extract(
        source: ProviderSource
    ): ExtractionResult {

        val document = ExtractorHelper.fetchDocument(
            source.url,
            source.headers
        )

        val candidates = linkedSetOf<String>()

        // ---------- Links ----------

        document.select(
            "a.btn, a[class*=btn], a[href]"
        ).forEach {

            val url = it.absUrl("href")

            if (url.isNotBlank()) {
                candidates += url
            }
        }

        // ---------- Video ----------

        document.select(
            "video source[src]"
        ).forEach {

            val url = it.absUrl("src")

            if (url.isNotBlank()) {
                candidates += url
            }
        }

        // ---------- iframe ----------

        document.select(
            "iframe[src]"
        ).forEach {

            val url = it.absUrl("src")

            if (url.isNotBlank()) {
                candidates += url
            }
        }

        // ---------- JavaScript ----------

        document.select("script")
            .forEach {

                ExtractorUtils
                    .allMatches(
                        """https?:\/\/[^\s"'<>\\]+""",
                        it.data()
                    )
                    .forEach(candidates::add)
            }

        if (candidates.isEmpty()) {
            return ExtractionResult()
        }

        val nextSources = mutableListOf<ProviderSource>()

        candidates.forEach { url ->

            val lower = url.lowercase()

            // Skip garbage

            if (
                lower.startsWith("javascript:") ||
                lower == "#" ||
                lower == source.url.lowercase()
            ) {
                return@forEach
            }

            if (
                lower.contains("facebook") ||
                lower.contains("twitter") ||
                lower.contains("telegram") ||
                lower.contains("discord")
            ) {
                return@forEach
            }

            val type =
                if (
                    lower.contains("go.php") ||
                    lower.contains("download.php") ||
                    lower.contains("redirect.php") ||
                    lower.contains("gamerxyt")
                ) {
                    HostType.REDIRECT
                } else {
                    HostDetector.detect(url)
                }

            nextSources += source.copy(
                url = url,
                hostType = type
            )
        }

        return ExtractionResult(
            sources = nextSources.distinctBy { it.url }
        )
    }
}