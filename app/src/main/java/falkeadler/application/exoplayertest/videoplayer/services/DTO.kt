package falkeadler.application.exoplayertest.videoplayer.services

import com.google.gson.annotations.SerializedName

data class SearchMovie(
    @SerializedName("poster_path")
    val posterPath: String? = null,
    val adult: Boolean? = null,
    val overview: String? = null,
    @SerializedName("release_date")
    val releaseDate: String? = null,
    @SerializedName("genre_ids")
    val genreIds: List<Int>? = null,
    val id: Int? = null,
    @SerializedName("original_title")
    val originalTitle: String? = null,
    @SerializedName("original_language")
    val originalLanguage: String? = null,
    val title: String? = null,
    @SerializedName("backdrop_path")
    val backdropPath: String? = null,
    val popularity: Double? = null,
    @SerializedName("vote_count")
    val voteCount: Int? = null,
    val video: Boolean? = null,
    @SerializedName("vote_average")
    val voteAverage: Double? = null
) {
    fun extract() : SearchedItem {
        return SearchedItem(
            id = id ?: -1,
            releaseDate = releaseDate ?: "",
            title = title ?: ""
        )
    }
}

data class ResponseSearchMovie(
    val page: Int? = null,
    val results: List<SearchMovie>? = null,
    @SerializedName("total_results")
    val totalResults: Int? = null,
    @SerializedName("total_pages")
    val totalPages: Int? = null
)


data class Genre(
    val id: Int? = null,
    val name: String? = null,
)

data class ProductionCompany(
    val name: String? = null,
    val id: Integer? = null,
    @SerializedName("logo_path")
    val logoPath: String? = null,
    @SerializedName("origin_country")
    val originCountry: String? = null
)

data class ResponseMovie(
    val adult: Boolean? = null,
    @SerializedName("backdrop_path")
    val backdropPath: String? = null,
    val genres: List<Genre>? = null,
    val homepage: String? = null,
    val id: Int? = null,
    @SerializedName("imdb_id")
    val imdbId: String? = null,
    @SerializedName("original_title")
    val originalTitle: String? = null,
    val overview: String? = null,
    val popularity: Double? = null,
    @SerializedName("poster_path")
    val posterPath: String? = null,
    @SerializedName("production_companies")
    val productionCompanies: List<ProductionCompany>? = null,
    @SerializedName("release_date")
    val releaseDate: String? = null,
    val runtime: Int? = null,
    val status: String? = null,
    val title: String? = null,
    @SerializedName("vote_count")
    val voteCount: Int? = null,
    val video: Boolean? = null,
    @SerializedName("vote_average")
    val voteAverage: Double? = null,
    val revenue: Int? = null
) {
    fun extract() : MovieItem {
        return MovieItem(
            id = id ?: -1,
            imdbId = imdbId ?: "",
            posterPath = posterPath ?: "",
            overview = overview ?: "",
            productionCompanies = productionCompanies ?: listOf(),
            runtime = runtime ?: -1,
            releaseDate = releaseDate ?: "",
            title = title ?: "",
            genres = genres ?: listOf()
        )
    }
}

data class MovieItem(
    val id: Int,
    val imdbId: String,
    val posterPath: String,
    val overview: String,
    val productionCompanies: List<ProductionCompany>,
    val runtime: Int,
    val releaseDate: String,
    val title: String,
    val genres: List<Genre>
)

data class SearchedItem(
    val id: Int,
    val title:String,
    val releaseDate: String
)

data class ImageConfig(
    @SerializedName("base_url")
    val baseUrl: String,
    @SerializedName("secure_base_url")
    val secureBaseUrl: String,
    @SerializedName("backdrop_sizes")
    val backdropSizes: List<String>,
    @SerializedName("logo_sizes")
    val logoSizes: List<String>,
    @SerializedName("poster_sizes")
    val posterSizes: List<String>,
    @SerializedName("profile_sizes")
    val profileSizes: List<String>,
    @SerializedName("still_sizes")
    val stillSizes: List<String>)

data class ResponseConfiguration(
    val images: ImageConfig,
    @SerializedName("change_keys")
    val changeKeys: List<String>
)