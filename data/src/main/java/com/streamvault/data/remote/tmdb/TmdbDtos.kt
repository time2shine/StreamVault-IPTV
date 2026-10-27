package com.streamvault.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbSearchResponse(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val results: List<TmdbMovieSummary> = emptyList(),
    @SerialName("total_results") val totalResults: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0
)

@Serializable
data class TmdbMovieSummary(
    @SerialName("id") val id: Long = 0L,
    @SerialName("title") val title: String = "",
    @SerialName("overview") val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f
)

@Serializable
data class TmdbMovieDetailResponse(
    @SerialName("id") val id: Long = 0L,
    @SerialName("title") val title: String = "",
    @SerialName("overview") val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Float = 0f,
    @SerialName("genres") val genres: List<TmdbGenre> = emptyList(),
    @SerialName("credits") val credits: TmdbCredits? = null
)

@Serializable
data class TmdbGenre(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = ""
)

@Serializable
data class TmdbCredits(
    @SerialName("cast") val cast: List<TmdbCastMember> = emptyList(),
    @SerialName("crew") val crew: List<TmdbCrewMember> = emptyList()
)

@Serializable
data class TmdbCastMember(
    @SerialName("name") val name: String = "",
    @SerialName("character") val character: String = ""
)

@Serializable
data class TmdbCrewMember(
    @SerialName("name") val name: String = "",
    @SerialName("job") val job: String = ""
)

data class TmdbMovieResult(
    val id: Long,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String?,
    val rating: Float,
    val genres: String?,
    val cast: String?,
    val director: String?,
    val year: String?
)
