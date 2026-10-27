package com.streamvault.data.remote.tmdb

import com.streamvault.data.BuildConfig
import com.streamvault.data.remote.http.useCancellableResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TmdbApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @param:Named("tmdbApiKey") private val injectedApiKey: String? = null,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val apiKey: String
        get() = injectedApiKey?.takeIf { it.isNotBlank() } ?: BuildConfig.TMDB_API_KEY

    suspend fun getMovieDetails(tmdbId: Long, language: String? = null): TmdbMovieResult? = withContext(Dispatchers.IO) {
        if (tmdbId <= 0 || apiKey.isBlank()) return@withContext null

        val langParam = buildLanguageParam(language)
        val url = "https://api.themoviedb.org/3/movie/$tmdbId?api_key=$apiKey&append_to_response=credits$langParam"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            okHttpClient.newCall(request).useCancellableResponse { response ->
                if (!response.isSuccessful) return@useCancellableResponse null
                val body = response.body?.string() ?: return@useCancellableResponse null
                val detail = json.decodeFromString<TmdbMovieDetailResponse>(body)
                detail.toMovieResult()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchMovie(title: String, year: Int? = null, language: String? = null): TmdbMovieResult? = withContext(Dispatchers.IO) {
        if (title.isBlank() || apiKey.isBlank()) return@withContext null

        // 1. Search with year if available
        if (year != null && year > 1900) {
            val result = executeSearch(title, year, language)
            if (result != null) return@withContext result
        }

        // 2. Search without year as fallback
        executeSearch(title, null, language)
    }

    private suspend fun executeSearch(title: String, year: Int?, language: String?): TmdbMovieResult? {
        val encodedQuery = URLEncoder.encode(title, "UTF-8")
        val yearParam = if (year != null && year > 1900) "&primary_release_year=$year" else ""
        val langParam = buildLanguageParam(language)
        val url = "https://api.themoviedb.org/3/search/movie?api_key=$apiKey&query=$encodedQuery$yearParam$langParam"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            okHttpClient.newCall(request).useCancellableResponse { response ->
                if (!response.isSuccessful) return@useCancellableResponse null
                val body = response.body?.string() ?: return@useCancellableResponse null
                val searchResponse = json.decodeFromString<TmdbSearchResponse>(body)
                val topResult = searchResponse.results.firstOrNull() ?: return@useCancellableResponse null
                topResult.toMovieResult()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildLanguageParam(language: String?): String {
        val lang = language?.takeIf { it.isNotBlank() } ?: Locale.getDefault().toLanguageTag()
        return if (lang.isNotBlank()) "&language=$lang" else ""
    }

    private fun TmdbMovieDetailResponse.toMovieResult(): TmdbMovieResult {
        val directorName = credits?.crew?.firstOrNull { it.job.equals("Director", ignoreCase = true) }?.name
        val castList = credits?.cast?.take(5)?.joinToString(", ") { it.name }?.ifBlank { null }
        val genreList = genres.map { it.name }.filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }
        val releaseYear = releaseDate?.take(4)?.takeIf { it.all(Char::isDigit) }

        return TmdbMovieResult(
            id = id,
            title = title,
            overview = overview,
            posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" },
            releaseDate = releaseDate,
            rating = voteAverage,
            genres = genreList,
            cast = castList,
            director = directorName,
            year = releaseYear
        )
    }

    private fun TmdbMovieSummary.toMovieResult(): TmdbMovieResult {
        val releaseYear = releaseDate?.take(4)?.takeIf { it.all(Char::isDigit) }

        return TmdbMovieResult(
            id = id,
            title = title,
            overview = overview,
            posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" },
            releaseDate = releaseDate,
            rating = voteAverage,
            genres = null,
            cast = null,
            director = null,
            year = releaseYear
        )
    }
}
