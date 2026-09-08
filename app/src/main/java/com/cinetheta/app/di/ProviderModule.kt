package com.cinetheta.app.di

import com.cinetheta.domain.provider.Provider
import com.cinetheta.domain.repositories.ProviderRepository
import com.cinetheta.providers.hdhub4u.HDHubProvider
import com.cinetheta.providers.netmirror.NetMirrorProvider
import com.cinetheta.providers.moviebox.MovieBoxProvider
import com.cinetheta.providers.fourkhdhub.FourKHDHubProvider
import com.cinetheta.providers.animedekho.AnimeDekhoProvider
import com.cinetheta.providers.toonstream.ToonStreamProvider

/**
 * Dependency module for streaming providers.
 *
 * This module is responsible for creating every provider
 * used by the application and exposing a shared
 * ProviderRepository.
 */
object ProviderModule {

    /**
     * Registered providers.
     *
     * Add new providers here as they are implemented.
     */
    val providers: List<Provider> by lazy {

        listOf(
            MovieBoxProvider(),
            NetMirrorProvider(),
            AnimeDekhoProvider(),
            ToonStreamProvider(),
            HDHubProvider(),
            FourKHDHubProvider()
        )
    }

    /**
     * Shared ProviderRepository.
     */
    val repository: ProviderRepository by lazy {

        ProviderRepository(
            providers = providers
        )

    }
}
