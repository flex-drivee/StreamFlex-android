package com.cinetheta.app.ui.pluginsearch

import com.cinetheta.domain.models.ProviderSource

object PluginSharedData {
    var directSources: List<ProviderSource>? = null
    
    fun takeSources(): List<ProviderSource>? {
        val s = directSources
        directSources = null
        return s
    }
}
