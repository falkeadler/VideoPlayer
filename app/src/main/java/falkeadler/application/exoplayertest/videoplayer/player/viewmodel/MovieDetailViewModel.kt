package falkeadler.application.exoplayertest.videoplayer.player.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import falkeadler.application.exoplayertest.videoplayer.BuildConfig
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.URLEncoder
import kotlin.math.abs

class MovieDetailViewModel: ViewModel() {
    private interface MovieInformationInterface {
        @GET("search/movie")
        suspend fun searchMovie(@Query("query") queryString: String,
                                @Query("api_key") apiKey:String = BuildConfig.TMDB_API_KEY,
                                @Query("include_adult") includeAdult: Boolean = true): ResponseSearchMovie
        @GET("search/movie")
        suspend fun searchMovieWithPage(@Query("query") queryString: String,
                                        @Query("page") page: Int,
                                        @Query("api_key") apiKey:String = BuildConfig.TMDB_API_KEY,
                                        @Query("include_adult") includeAdult: Boolean = true): ResponseSearchMovie

        @GET("movie/{movie_id}")
        suspend fun getMovieDetail(@Path("movie_id") movieId: String,
                                   @Query("api_key") apiKey:String = BuildConfig.TMDB_API_KEY
        ): ResponseMovie

        @GET("configuration")
        suspend fun apiConfiguration(@Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY): ResponseConfiguration
    }


    private val movieInformationService: MovieInformationInterface by lazy {
        val logClient = OkHttpClient().newBuilder().addInterceptor(HttpLoggingInterceptor(
            HttpLoggingInterceptor.Logger {
                L.e(it)
            }).apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }).addInterceptor(HttpLoggingInterceptor(
            HttpLoggingInterceptor.Logger {
                L.e(it)
            }).apply {
            level = HttpLoggingInterceptor.Level.BODY
        }).build()
        Retrofit.Builder().baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }.asConverterFactory("application/json".toMediaTypeOrNull()!!))
            .client(logClient).build().create(MovieInformationInterface::class.java)
    }

    private val _movieItem = MutableLiveData<MovieItem>()
    val movieItem: LiveData<MovieItem> = _movieItem
    private var totalPages = 0
    private var currentPage = -1

    private val _searchedItemFlow = MutableSharedFlow<SearchedItem>(
        replay = 1,
        extraBufferCapacity = 20,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val searchedItemFlow = _searchedItemFlow.asSharedFlow()
    private val loadingItem = SearchedItem(-1, "", "")
    private val cancelItem = SearchedItem(-2, "", "")
    fun queryByTitle(data: String, nextPage: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (nextPage && (totalPages < 2 || currentPage >= totalPages)) {
                L.e("[SCROLL] no more contents! totalPages = $totalPages || currentPage = $currentPage")
            } else {
                _searchedItemFlow.emit(loadingItem)
                val searchedMovies = if (nextPage) {
                    movieInformationService.searchMovieWithPage(
                        URLEncoder.encode(
                            "Dark Knight",
                            "utf-8"
                        ), currentPage + 1
                    )
                } else {
                    movieInformationService.searchMovie(URLEncoder.encode("Dark Knight", "utf-8"))
                }
                //val searchedMovies = searchByTitle(data.title)
                totalPages = searchedMovies.totalPages
                currentPage = searchedMovies.page
                if (searchedMovies.results.isEmpty()) {
                    _searchedItemFlow.emit(cancelItem)
                } else {
                    L.e("[SCROLL] searchedItemCount = ${searchedMovies.results.size}")
                    searchedMovies.results.forEach {
                        _searchedItemFlow.emit(it.extract())
                    }
                }
            }
        }
    }

    fun getDetail(tmdbId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = movieInformationService.getMovieDetail(tmdbId.toString())
            details.let {
                val imageConfig = movieInformationService.apiConfiguration().images
                var index = -1
                var diffMax = Int.MAX_VALUE
                imageConfig.posterSizes.forEachIndexed { idx, sizeStr ->
                    if (sizeStr.startsWith("w") || sizeStr.startsWith("W")) {
                        val diff = abs(sizeStr.substring(1).toInt() - 200)
                        if (diff < diffMax) {
                            index = idx
                        }
                    }
                }
                val sizeStr = if (index == -1) "original" else imageConfig.posterSizes[index]
                val totalPath = "${imageConfig.secureBaseUrl}$sizeStr${it.posterPath}"
                val newData = it.extract()
                _movieItem.postValue(newData.copy(posterPath = totalPath))
            }
        }
    }
}
