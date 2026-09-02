package com.streamflex.app.di

import com.streamflex.domain.provider.Provider
import com.streamflex.domain.repositories.ProviderRepository
import com.streamflex.providers.hdhub4u.HDHubProvider
import com.streamflex.providers.netmirror.NetMirrorProvider
import com.streamflex.providers.moviebox.MovieBoxProvider
import com.streamflex.providers.fourkhdhub.FourKHDHubProvider
import com.streamflex.providers.animedekho.AnimeDekhoProvider
import com.streamflex.providers.toonstream.ToonStreamProvider

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

            HDHubProvider(),
            FourKHDHubProvider(),

            NetMirrorProvider(),
            
            AnimeDekhoProvider(),
                        ToonStreamProvider()
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
