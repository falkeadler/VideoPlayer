package falkeadler.library.youtubedataextractor

import android.content.Context
import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.net.URLDecoder

object YouTubeDataExtractor {
    object ITAG_TYPES {
        val DASH_VIDEO = setOf(160, 133, 134, 135, 136, 137, 264, 266, 298, 299, 278, 242, 243, 244, 247, 248, 271, 313, 302, 308, 303, 315)
        val DASH_AUDIO = setOf(140, 141, 256, 258, 171, 249, 250, 251)
        val OLD_SCHOOL = setOf(5, 17, 36, 43, 18, 22)
        val HLS = setOf(91, 92, 93, 94, 95, 96)
    }
    private interface YouTubeService {
        @GET("watch")
        suspend fun getYouTubePageHtml(@Query("v") videoId: String): ResponseBody
    }


    private val headerClient = OkHttpClient().newBuilder().addInterceptor(
        Interceptor {
            with(it) {
                val newRequest = request().newBuilder().addHeader("User-Agent", USER_AGENT).build()
                proceed(newRequest)
            }
        }
    ).build()

    private val logClient = OkHttpClient().newBuilder().addInterceptor(HttpLoggingInterceptor(
        HttpLoggingInterceptor.Logger {
            Log.e("VidePlayerTestTAG", it)
        }).apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }).build()

    private val service: YouTubeService = Retrofit.Builder().baseUrl("https://www.youtube.com/")
        .client(headerClient).client(logClient).build().create(YouTubeService::class.java)

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.98 Safari/537.36"

    suspend fun extract(videoId: String, context: Context): YouTubeData {
        var youTubeData = YouTubeData()
        if (videoId.isNotEmpty() && (videoId.length == 10 || videoId.length == 11)) {
            val html = service.getYouTubePageHtml(videoId).string()
            val body = Jsoup.parse(html).body()
            val dataString = body.getElementsByTag("script").asSequence().filter { element ->
                element.data().startsWith("var ytInitialPlayerResponse")
            }.take(1).first().data()
            val decipherJSFile = body.getElementsByTag("script").asSequence().filter {
                element ->

                if (element.hasAttr("src")) {
                    val attrValue = element.attr("src")
                    attrValue.startsWith("/s/player/") && attrValue.endsWith(".js")
                } else {
                    false
                }
            }.map { it.attr("src") }.take(1).first().substring(1)
            Log.e("VideoPlayerTestTAG", "decipher file = $decipherJSFile")
            val extractor = DecipherMethodExtractor(decipherJSFile, context)
            val jsonStart = dataString.indexOfFirst { it == '{' }
            val jsonLast = dataString.indexOfLast { it == '}' }
            val jsonStr = dataString.substring(jsonStart, jsonLast + 1)
            val parser = Json {
                this.isLenient = true
                this.coerceInputValues = true
                this.ignoreUnknownKeys = true
            }
            val response = parser.decodeFromString<YTPlayerResponse>(jsonStr)
            youTubeData = youTubeData.copy(
                videoId = response.videoDetails.videoId,
                title = response.videoDetails.title,
                author = response.videoDetails.author,
                lengthSeconds = response.videoDetails.lengthSeconds.toLong(),
                shortDescription = response.videoDetails.shortDescription,
                isLiveContent = response.videoDetails.isLiveContent,
                thumbnail = response.videoDetails.thumbnail.thumbnails.fold(response.videoDetails.thumbnail.thumbnails.first()) {
                        acc, thumbnailItem ->
                    if (acc.height < thumbnailItem.height) {
                        thumbnailItem
                    } else {
                        acc
                    }
                }
            )
            val videoList = response.streamingData.adaptiveFormats.filter {
                ITAG_TYPES.DASH_VIDEO.contains(it.itag)
            }.map {
                val url = if (it.url.isEmpty()) {
                    if (it.signatureCipher.isNotEmpty()) {
                        extractor.decipher(it.signatureCipher) ?: ""
                    } else {
                        ""
                    }
                } else {
                    URLDecoder.decode(it.url, "utf-8")
                }
                ItemVideo(
                    itag = it.itag,
                    url = url,
                    width = it.width,
                    height = it.height,
                    averageBitrate = it.averageBitrate,
                    approxDurationMs = it.approxDurationMs.toLong()
                )
            }
            val audioList = response.streamingData.adaptiveFormats.filter {
                ITAG_TYPES.DASH_AUDIO.contains(it.itag)
            }.map {
                val url = if (it.url.isEmpty()) {
                    if (it.signatureCipher.isNotEmpty()) {
                        extractor.decipher(it.signatureCipher) ?: ""
                    } else {
                        ""
                    }
                } else {
                    URLDecoder.decode(it.url, "utf-8")
                }
                ItemAudio(
                    itag = it.itag,
                    url = url,
                    audioSampleRate = it.audioSampleRate,
                    audioChannels = it.audioChannels,
                    loudnessDb = it.loudnessDb,
                    approxDurationMs = it.approxDurationMs.toLong()
                )
            }
            val eitherList = response.streamingData.formats.filter {
                ITAG_TYPES.OLD_SCHOOL.contains(it.itag)
            }.map {
                val url = if (it.url.isEmpty()) {
                    if (it.signatureCipher.isNotEmpty()) {
                        extractor.decipher(it.signatureCipher) ?: ""
                    } else {
                        ""
                    }
                } else {
                    URLDecoder.decode(it.url, "utf-8")
                }
                ItemEither(
                    itag = it.itag,
                    url = url,
                    audioSampleRate = it.audioSampleRate,
                    audioChannels = it.audioChannels,
                    loudnessDb = it.loudnessDb,
                    approxDurationMs = it.approxDurationMs.toLong(),
                    width = it.width,
                    height = it.height,
                    averageBitrate = it.averageBitrate
                )
            }

            val hlsList = response.streamingData.formats.filter {
                ITAG_TYPES.HLS.contains(it.itag)
            }.map {
                val url = if (it.url.isEmpty()) {
                    if (it.signatureCipher.isNotEmpty()) {
                        extractor.decipher(it.signatureCipher) ?: ""
                    } else {
                        ""
                    }
                } else {
                    URLDecoder.decode(it.url, "utf-8")
                }
                ItemEither(
                    itag = it.itag,
                    url = url,
                    audioSampleRate = it.audioSampleRate,
                    audioChannels = it.audioChannels,
                    loudnessDb = it.loudnessDb,
                    approxDurationMs = it.approxDurationMs.toLong(),
                    width = it.width,
                    height = it.height,
                    averageBitrate = it.averageBitrate
                )
            }

            youTubeData = youTubeData.copy(
                videos = videoList.toList(),
                audios = audioList.toList(),
                oldSchool = eitherList.toList(),
                hlsStream = hlsList.toList()
            )
        }
        return youTubeData
    }
}