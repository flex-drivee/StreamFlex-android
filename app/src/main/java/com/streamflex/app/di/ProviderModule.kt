package com.streamflex.app.di

import com.streamflex.domain.provider.Provider
import com.streamflex.domain.repositories.ProviderRepository
import com.streamflex.providers.hdhub4u.HDHubProvider
import com.streamflex.providers.netmirror.NetflixMirrorProvider
import com.streamflex.providers.netmirror.PrimeVideoMirrorProvider
import com.streamflex.providers.netmirror.HotStarMirrorProvider
import com.streamflex.providers.netmirror.DisneyPlusMirrorProvider
import com.streamflex.providers.moviebox.MovieBoxProvider

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

            HDHubProvider(),

            NetflixMirrorProvider(),
            PrimeVideoMirrorProvider(),
            HotStarMirrorProvider(),
            DisneyPlusMirrorProvider(),

            // Future:
            MovieBoxProvider()
            // OTTMirrorProvider()
            // NetMirrorProvider()
            // AnimeProvider()

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