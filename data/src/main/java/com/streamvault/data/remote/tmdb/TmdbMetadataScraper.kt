package com.streamvault.data.remote.tmdb

import com.streamvault.domain.model.Movie
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbMetadataScraper @Inject constructor(
    private val tmdbApiClient: TmdbApiClient
) {
    private val attemptedMovieIds = ConcurrentHashMap.newKeySet<Long>()

    fun hasAttempted(movieId: Long): Boolean = movieId in attemptedMovieIds

    fun markAttempted(movieId: Long) {
        attemptedMovieIds.add(movieId)
    }

    suspend fun enrichMovie(movie: Movie, systemLanguage: String? = null): Movie {
        if (movie.id > 0) {
            attemptedMovieIds.add(movie.id)
        }

        // If movie already has plot, backdrop, and rating, no need to query TMDB
        if (!movie.plot.isNullOrBlank() && !movie.backdropUrl.isNullOrBlank() && movie.rating > 0f) {
            return movie
        }

        val existingTmdbId = movie.tmdbId
        val tmdbResult: TmdbMovieResult? = if (existingTmdbId != null && existingTmdbId > 0L) {
            tmdbApiClient.getMovieDetails(existingTmdbId, systemLanguage)
        } else {
            val sanitized = TmdbTitleSanitizer.sanitize(movie.name)
            if (sanitized.cleanTitle.isNotBlank()) {
                tmdbApiClient.searchMovie(sanitized.cleanTitle, sanitized.year, systemLanguage)
            } else {
                null
            }
        }

        if (tmdbResult == null) return movie

        return movie.copy(
            plot = movie.plot?.takeIf { it.isNotBlank() } ?: tmdbResult.overview.takeIf { it.isNotBlank() },
            backdropUrl = movie.backdropUrl?.takeIf { it.isNotBlank() } ?: tmdbResult.backdropUrl,
            posterUrl = movie.posterUrl?.takeIf { it.isNotBlank() } ?: tmdbResult.posterUrl,
            tmdbId = movie.tmdbId ?: tmdbResult.id.takeIf { it > 0 },
            genre = movie.genre?.takeIf { it.isNotBlank() } ?: tmdbResult.genres,
            cast = movie.cast?.takeIf { it.isNotBlank() } ?: tmdbResult.cast,
            director = movie.director?.takeIf { it.isNotBlank() } ?: tmdbResult.director,
            rating = if (movie.rating > 0f) movie.rating else tmdbResult.rating,
            year = movie.year ?: tmdbResult.year
        )
    }
}
