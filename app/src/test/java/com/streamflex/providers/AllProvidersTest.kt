package com.streamflex.providers

import com.streamflex.domain.models.MediaType
import com.streamflex.domain.provider.Provider
import com.streamflex.providers.animedekho.AnimeDekhoProvider
import com.streamflex.providers.fourkhdhub.FourKHDHubProvider
import com.streamflex.providers.hdhub4u.HDHubProvider
import com.streamflex.providers.moviebox.MovieBoxProvider
import com.streamflex.providers.netmirror.NetMirrorProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AllProvidersTest {

    @Test
    fun testAllProviders() = runBlocking {
        val providers = listOf(
            AnimeDekhoProvider(),
            FourKHDHubProvider(),
            HDHubProvider(),
            MovieBoxProvider(),
            NetMirrorProvider()
        )

        for (provider in providers) {
            println("=========================================")
            println("Testing Provider: ${provider.name}")
            println("=========================================")
            try {
                // Test Search
                val searchResults = provider.search("spider", MediaType.MOVIE)
                if (searchResults.isEmpty()) {
                    println("[!] No search results for ${provider.name}")
                    continue
                }
                println("[+] Found ${searchResults.size} search results.")
                
                val first = searchResults.first()
                println("[+] Selecting: ${first.title} (${first.id})")

                // Test Load Details
                val details = provider.loadDetails(first.id, first.url, first.type)
                if (details == null) {
                    println("[!] Failed to load details for ${provider.name}")
                    continue
                }
                
                println("[+] Details loaded successfully. Extracted streams/episodes.")
                println("[+] Extracted ${details.streamLinks.size} movie streams.")
                println("[+] Extracted ${details.episodes.size} tv episodes.")
                
                if (details.streamLinks.isNotEmpty()) {
                    val stream = details.streamLinks.first()
                    println("[+] First stream link: ${stream.name} - ${stream.url}")
                }
                
                if (details.episodes.isNotEmpty()) {
                    val ep = details.episodes.first()
                    println("[+] First episode: S${ep.season}E${ep.episode} - ${ep.title}")
                    
                    if (ep.streamLinks.isNotEmpty()) {
                        val stream = ep.streamLinks.first()
                        println("[+] First episode stream link: ${stream.name} - ${stream.url}")
                    }
                }
                
                println("[+] Provider ${provider.name} is working correctly.")
            } catch (e: Exception) {
                println("[ERROR] Exception in provider ${provider.name}: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
