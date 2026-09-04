package com.streamflex.app.data.metadata

import com.streamflex.app.domain.models.*

object TmdbMapper {

    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
    private const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780"

    // --- Search Mappers ---

    fun toDomain(tmdbResult: TmdbSearchResult): SearchResult {
        val type = when (tmdbResult.mediaType) {
            "tv" -> ContentType.SHOW
            "movie" -> ContentType.MOVIE
            else -> if (tmdbResult.tvName != null) ContentType.SHOW else ContentType.MOVIE
        }

        val title = tmdbResult.movieTitle ?: tmdbResult.tvName ?: "Unknown Title"
        val date = tmdbResult.releaseDate ?: tmdbResult.firstAirDate
        val year = date?.take(4)?.toIntOrNull()

        return SearchResult(
            id = tmdbResult.id.toString(),
            title = title,
            // FIX: Added brackets around variable name to prevent string interpolation errors
            poster = tmdbResult.posterPath?.let { "${IMAGE_BASE_URL}$it" },
            type = type,
            year = year
        )
    }

    // --- Movie Mappers ---

    fun toDomain(details: TmdbMovieDetails): Movie {
        val year = details.releaseDate?.take(4)?.toIntOrNull()

        return Movie(
            id = details.id.toString(),
            title = details.title ?: "Unknown",
            overview = details.overview,
            poster = details.posterPath?.let { "${IMAGE_BASE_URL}$it" },
            backdrop = details.backdropPath?.let { "${BACKDROP_BASE_URL}$it" },
            year = year,
            rating = details.voteAverage,
            runtime = details.runtime,
            genres = details.genres?.map { it.name } ?: emptyList(),
            providerSources = emptyList(),
            streams = emptyList(),
            cast = mapCast(details.credits),
            trailers = mapTrailers(details.videos),
            productionCompanies = mapCompanies(details.productionCompanies)
        )
    }

    // --- Show Mappers ---

    fun toDomain(details: TmdbShowDetails): Show {
        val year = details.firstAirDate?.take(4)?.toIntOrNull()

        return Show(
            id = details.id.toString(),
            title = details.name ?: "Unknown",
            overview = details.overview,
            poster = details.posterPath?.let { "${IMAGE_BASE_URL}$it" },
            backdrop = details.backdropPath?.let { "${BACKDROP_BASE_URL}$it" },
            year = year,
            rating = details.voteAverage,
            genres = details.genres?.map { it.name } ?: emptyList(),
            seasons = details.seasons?.map { toDomain(it) } ?: emptyList(),
            cast = mapCast(details.credits),
            trailers = mapTrailers(details.videos),
            productionCompanies = mapCompanies(details.productionCompanies)
        )
    }

    // Helper for Season
    private fun toDomain(season: TmdbSeason): Season {
        return Season(
            seasonNumber = season.seasonNumber,
            episodes = emptyList()
        )
    }

    private fun mapCast(credits: TmdbCredits?): List<CastMember> {
        return credits?.cast?.map {
            CastMember(
                name = it.name,
                character = it.character,
                imageUrl = it.profilePath?.let { path -> "${IMAGE_BASE_URL}$path" }
            )
        } ?: emptyList()
    }

    private fun mapTrailers(videos: TmdbVideos?): List<Trailer> {
        return videos?.results?.filter { it.site == "YouTube" && it.type == "Trailer" }?.map {
            Trailer(key = it.key, name = it.name)
        } ?: emptyList()
    }

    private fun mapCompanies(companies: List<TmdbProductionCompany>?): List<ProductionCompany> {
        return companies?.map {
            ProductionCompany(
                name = it.name,
                logoUrl = it.logoPath?.let { path -> "${IMAGE_BASE_URL}$path" }
            )
        } ?: emptyList()
    }
}
