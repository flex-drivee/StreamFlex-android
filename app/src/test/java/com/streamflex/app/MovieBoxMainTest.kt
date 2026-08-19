package com.streamflex.app

import com.streamflex.providers.moviebox.MovieBoxSearch
import com.streamflex.providers.moviebox.MovieBoxDetails
import com.streamflex.extractors.moviebox.MovieBoxExtractor
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("Starting Search...")
    val searcher = MovieBoxSearch()
    val results = searcher.search("Spider-Man No Way Home", "https://api3.aoneroom.com")
    
    println("Found ${results.size} results.")
    results.forEach { println("Result: ${it.title} (ID: ${it.id})") }
    
    val first = results.firstOrNull()
    if (first == null) {
        println("No results found.")
        return@runBlocking
    }
    
    println("Fetching details for: ${first.title} (ID: ${first.id})")
    val details = MovieBoxDetails()
    val providerResult = details.load(first, "https://api3.aoneroom.com")
    
    if (providerResult == null) {
        println("No details found.")
        return@runBlocking
    }
    
    println("Extracting streams for play info...")
    val extractor = MovieBoxExtractor()
    
    val source = providerResult.sources.firstOrNull()
    if (source == null) {
        println("No provider sources found.")
        return@runBlocking
    }
    
    val streams = extractor.extract(source)
    println("Extracted ${streams.streams.size} streams.")
    streams.streams.forEach { stream ->
        println("Stream: ${stream.name} - ${stream.url}")
    }
}
