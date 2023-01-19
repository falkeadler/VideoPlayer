package falkeadler.application.exoplayertest.videoplayer.list.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.http.HttpRequestInitializer
import falkeadler.application.exoplayertest.videoplayer.L
import com.google.api.services.youtube.YouTube
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.model.SearchListResponse
import com.google.api.services.youtube.model.SearchResult
import falkeadler.application.exoplayertest.videoplayer.BuildConfig
import falkeadler.library.youtubedataextractor.YouTubeData
import falkeadler.library.youtubedataextractor.YouTubeDataExtractor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentLinkedQueue

class YoutubeViewModel(app: Application): AndroidViewModel(app) {

    var searchText = ""

    private val httpTransport = NetHttpTransport()
    private val jsonFactory = GsonFactory()
    private val youtube: YouTube = YouTube.Builder(httpTransport, jsonFactory, HttpRequestInitializer {

    }).setApplicationName("videoplayer-374900").build()

    private var cacheSearchText = ""
    private val cacheSearchResultQueue = ConcurrentLinkedQueue<String>()

    private val _youtubeItemFlow = MutableSharedFlow<YouTubeData>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val youtubeItemFlow = _youtubeItemFlow.asSharedFlow()

    var searchDef: Deferred<SearchListResponse>? = null

    private val _loadingStateFlow = MutableStateFlow(false)
    val loadingStateFlow = _loadingStateFlow.asStateFlow()


    private suspend fun queryByText(): List<SearchResult> {
        if (searchDef?.isActive == true) {
            searchDef?.cancelAndJoin()
        }
        if (cacheSearchText.isNotEmpty()) {
            searchDef = viewModelScope.async(Dispatchers.IO) {
                youtube.search().list(listOf("id"))
                    .setKey(BuildConfig.YOUTUBE_API_KEY)
                    .setQ(cacheSearchText).setType(listOf("video")).setMaxResults(50).execute()
            }
            return searchDef!!.await().items
        }
        return listOf()
    }

    private var requestJob: Job? = null
    fun request() {
        if (requestJob?.isActive == true && requestJob?.isCompleted == false) {
            requestJob?.cancel()
            requestJob = null
        }
        if (searchText != cacheSearchText) {
            cacheSearchResultQueue.clear()
        }
        if (searchText.isNotEmpty()) {
            cacheSearchText = searchText
        }
        requestJob = viewModelScope.launch(Dispatchers.Default) {
            _loadingStateFlow.emit(true)
            if (cacheSearchResultQueue.isEmpty()) {
                cacheSearchResultQueue.addAll(queryByText().map { it.id.videoId })
            }
            val buildCount = kotlin.math.min(10, cacheSearchResultQueue.size)

            repeat(buildCount) {
                val item = cacheSearchResultQueue.poll()
                val extracted = YouTubeDataExtractor.extract(item, context = getApplication<Application?>().applicationContext)
                _youtubeItemFlow.emit(extracted)
            }
            _loadingStateFlow.emit(false)
        }
    }

    fun stopRequest() {
        if (requestJob?.isActive == true && requestJob?.isCompleted == false) {
            requestJob?.cancel()
        }
        viewModelScope.launch {
            _loadingStateFlow.emit(false)
        }
    }
}