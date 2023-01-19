package falkeadler.application.exoplayertest.videoplayer.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SearchMovie(
    @SerialName("poster_path")
    val posterPath: String = "",
    val overview: String = "",
    @SerialName("release_date")
    val releaseDate: String = "",
    @SerialName("genre_ids")
    val genreIds: List<Int> = listOf(),
    val id: Int = -1,
    @SerialName("original_title")
    val originalTitle: String = "",
    @SerialName("original_language")
    val originalLanguage: String = "",
    val title: String = "",
) {
    fun extract() : SearchedItem {
        return SearchedItem(
            id = id,
            releaseDate = releaseDate,
            title = title
        )
    }
}

@Serializable
data class ResponseSearchMovie(
    val page: Int = 0,
    val results: List<SearchMovie> = listOf(),
    @SerialName("total_results")
    val totalResults: Int = 0,
    @SerialName("total_pages")
    val totalPages: Int = 0)

@Serializable
data class Genre(
    val id: Int = -1,
    val name: String = "", )

@Serializable
data class ProductionCompany(
    val id: Int = -1,
    @SerialName("logo_path")
    val logoPath: String = "", )

@Serializable
data class ResponseMovie(
    val genres: List<Genre> = listOf(),
    val id: Int = -1,
    @SerialName("imdb_id")
    val imdbId: String = "",
    @SerialName("original_title")
    val originalTitle: String = "",
    val overview: String = "",
    @SerialName("poster_path")
    val posterPath: String = "",
    @SerialName("production_companies")
    val productionCompanies: List<ProductionCompany> = listOf(),
    @SerialName("release_date")
    val releaseDate: String = "",
    val runtime: Int = -1,
    val status: String = "",
    val title: String = ""
) {
    fun extract() : MovieItem {
        return MovieItem(
            id = id,
            imdbId = imdbId,
            posterPath = posterPath,
            overview = overview,
            productionCompanies = productionCompanies,
            runtime = runtime,
            releaseDate = releaseDate,
            title = title,
            genres = genres)
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
    val genres: List<Genre>)

data class SearchedItem(
    val id: Int,
    val title:String,
    val releaseDate: String)

@Serializable
data class ImageConfig(
    @SerialName("base_url")
    val baseUrl: String,
    @SerialName("secure_base_url")
    val secureBaseUrl: String,
    @SerialName("backdrop_sizes")
    val backdropSizes: List<String>,
    @SerialName("logo_sizes")
    val logoSizes: List<String>,
    @SerialName("poster_sizes")
    val posterSizes: List<String>,
    @SerialName("profile_sizes")
    val profileSizes: List<String>,
    @SerialName("still_sizes")
    val stillSizes: List<String>)

@Serializable
data class ResponseConfiguration(
    val images: ImageConfig,
    @SerialName("change_keys")
    val changeKeys: List<String>)