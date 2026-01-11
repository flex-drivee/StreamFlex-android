package com.streamflex.app.data.providers

import com.streamflex.app.core.BaseProvider

/**
 * 🧠 The "Plugin Manager"
 * This list tells the app which websites are installed and available to search.
 */
object ProviderManager {

    // The list of all active scrapers
    private val providers = mutableListOf<BaseProvider>()

    init {
        // ⚡ REGISTER YOUR SCRAPERS HERE ⚡
        // We just created HDHub4uProvider, so we add it to the list.
        register(HDHub4uProvider())
    }

    // Helper function to add a provider
    private fun register(provider: BaseProvider) {
        providers.add(provider)
    }

    // Used by Search Screen to find a specific provider (e.g., "HDHub4u")
    fun getProvider(name: String): BaseProvider? {
        return providers.find { it.name == name }
    }

    // Used by Home Screen to get everything
    fun getAll(): List<BaseProvider> = providers
}