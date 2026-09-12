package com.cinetheta.providers.moviebox

import com.cinetheta.core.parser.JsonParser
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.models.HomePageList
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonElement

object UniversalJsonScraper {

    private fun safeString(obj: JsonObject, key: String): String? {
        if (!obj.has(key)) return null
        val el = obj.get(key)
        return if (el.isJsonNull) null else if (el.isJsonPrimitive) el.asString else el.toString()
    }
    
    private fun safeInt(obj: JsonObject, key: String): Int? {
        if (!obj.has(key)) return null
        val el = obj.get(key)
        return if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else null
    }
    
    private fun extractItem(obj: JsonObject, baseUrl: String): SearchResult? {
        if (obj.has("subjectId") && obj.has("title")) {
            val id = safeString(obj, "subjectId") ?: ""
            val title = safeString(obj, "title") ?: ""
            
            if (id.isNotBlank() && title.isNotBlank()) {
                val coverObj = if (obj.has("cover") && obj.get("cover").isJsonObject) obj.get("cover").asJsonObject else null
                val poster = coverObj?.let { safeString(it, "url") } ?: safeString(obj, "poster")
                
                val typeStr = safeString(obj, "type") ?: "movie"
                val isTv = typeStr.equals("tv", ignoreCase = true) || safeInt(obj, "subjectType") == 2
                val mediaType = if (isTv) MediaType.TV else MediaType.MOVIE

                val releaseDate = safeString(obj, "releaseDate") ?: ""
                val year = releaseDate.substringBefore("-").toIntOrNull() ?: 0

                return MovieBoxMapper.toSearchResult(
                    id = id,
                    title = title,
                    detailUrl = "$baseUrl/wefeed-mobile-bff/subject-api/get?subjectId=$id",
                    poster = poster,
                    year = year,
                    mediaType = mediaType
                )
            }
        }
        return null
    }

    fun scrapeHomePage(jsonString: String, baseUrl: String): List<HomePageList> {
        val root = JsonParser.parse(jsonString) ?: return emptyList()
        val sections = mutableListOf<HomePageList>()
        
        // Sometimes the top level has a title
        var currentSectionTitle = "Featured"
        
        fun traverse(element: JsonElement?, parentTitle: String?) {
            if (element == null || element.isJsonNull) return
            
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                
                // Try to find a title for this block
                val title = safeString(obj, "title") ?: safeString(obj, "name") ?: safeString(obj, "moduleName") ?: parentTitle
                
                // Traverse all keys
                for (entry in obj.entrySet()) {
                    traverse(entry.value, title)
                }
            } else if (element.isJsonArray) {
                val arr = element.asJsonArray
                if (arr.size() == 0) return
                
                // Check if this array is a list of media items
                val firstItem = arr[0]
                if (firstItem.isJsonObject && firstItem.asJsonObject.has("subjectId") && firstItem.asJsonObject.has("title")) {
                    val items = mutableListOf<SearchResult>()
                    for (child in arr) {
                        if (child.isJsonObject) {
                            val item = extractItem(child.asJsonObject, baseUrl)
                            if (item != null) items.add(item)
                        }
                    }
                    if (items.isNotEmpty()) {
                        sections.add(HomePageList(parentTitle ?: "Trending", items.distinctBy { it.id }))
                    }
                } else {
                    // Traverse children
                    for (child in arr) {
                        traverse(child, parentTitle)
                    }
                }
            }
        }
        
        traverse(root, null)
        
        // Remove duplicates sections by title
        val uniqueSections = mutableListOf<HomePageList>()
        val seenTitles = mutableSetOf<String>()
        for (section in sections) {
            val title = section.title
            if (!seenTitles.contains(title)) {
                seenTitles.add(title)
                uniqueSections.add(section)
            } else {
                // If we see a duplicate title, append the items
                val existing = uniqueSections.find { it.title == title }
                if (existing != null) {
                    val combined = (existing.items + section.items).distinctBy { it.id }
                    uniqueSections[uniqueSections.indexOf(existing)] = HomePageList(title, combined)
                }
            }
        }
        
        return uniqueSections
    }
}
