package falkeadler.application.exoplayertest.videoplayer.services

import com.google.gson.GsonBuilder
import falkeadler.application.exoplayertest.videoplayer.L
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import kotlin.math.log

object TMDBService {
    const val APIKEY = "4bc72facd6c88514cb52a6d2e87fab9a"

    private val logClient = OkHttpClient().newBuilder().addInterceptor(HttpLoggingInterceptor(
        HttpLoggingInterceptor.Logger {
            L.e(it)
            println(it)
        }).apply {
            level = HttpLoggingInterceptor.Level.HEADERS
    }).addInterceptor(HttpLoggingInterceptor(
        HttpLoggingInterceptor.Logger {
            L.e(it)
            println(it)
        }).apply {
            level = HttpLoggingInterceptor.Level.BODY
    }).build()


    val service: TMDBServiceInterface = Retrofit.Builder().baseUrl("https://api.themoviedb.org/3/")
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        .client(logClient).build().create(TMDBServiceInterface::class.java)

    fun ImageServiceBuilder(url: String) = Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        .client(logClient)
}

interface TMDBImageServiceInterface {
    @GET("t/p/{size}/{posterPath}")
    suspend fun getPoster(@Path("size") posterSize: String,
    @Path("posterPath") posterPath: String): ResponseBody
}

interface TMDBServiceInterface {
    @GET("search/movie")
    suspend fun searchMovie(@Query("query") queryString: String,
                    @Query("api_key") apiKey:String = TMDBService.APIKEY,
                    @Query("include_adult") includeAdult: Boolean = true): ResponseSearchMovie

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(@Path("movie_id") movieId: String,
                       @Query("api_key") apiKey:String = TMDBService.APIKEY
    ): ResponseMovie

    @GET("configuration")
    suspend fun apiConfiguration(@Query("api_key") apiKey: String = TMDBService.APIKEY): ResponseConfiguration
}