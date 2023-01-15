package falkeadler.application.exoplayertest.videoplayer.list.viewmodel

import android.app.Application
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import at.huber.youtubeExtractor.YtFile
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.list.YouTubeUrlExtractor
import falkeadler.application.exoplayertest.videoplayer.list.YoutubeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class YoutubeViewModel(app: Application): AndroidViewModel(app) {

    private val _youtubeList = MutableLiveData<List<YoutubeData>>()
    val youtubeList: LiveData<List<YoutubeData>> = _youtubeList
    // 나중에 Data api 로 링크 가져올 예정 지금은 일단 그냥 임의로
    private val ytlist = listOf<String>(
        "https://www.youtube.com/watch?v=c1XJIopJXwo",
        "https://www.youtube.com/watch?v=GLUWHaUxQZw",
        "https://www.youtube.com/watch?v=xenP_cPq-2w",
        "https://www.youtube.com/watch?v=cL9EqZeHv2o",
        "https://www.youtube.com/watch?v=PxoMcB3GeCY",
        "https://www.youtube.com/watch?v=twV43MJi_fg",
        "https://www.youtube.com/watch?v=LhOGaRAXTzY"
    )
    private suspend fun parseList() {
        val newList = ytlist.mapNotNull { url ->
            var item: YoutubeData? =
                suspendCoroutine<YoutubeData?> { continuation ->
                    val extractor =
                        YouTubeUrlExtractor(getApplication<Application>().applicationContext)
                    extractor.setOnExtractionListener { sparseArray, videoMeta ->
                        val result = if (sparseArray != null && sparseArray.isNotEmpty()) {
                            var data = YoutubeData()
                            sparseArray.forEach { _, value ->
                                var heightMax = -1
                                var audioBitrateMax = -1
                                if (value != null) {
                                    if (value.format.height > -1 && value.format.audioBitrate > -1) {
                                        data = data.copy(avUrl = value.url)
                                    } else if (value.format.height == -1 && audioBitrateMax < value.format.audioBitrate) {
                                        data = data.copy(audioUrl = value.url, isAudioHls = value.format.isHlsContent, isAudioDash = value.format.isDashContainer)
                                    } else if (value.format.audioBitrate == -1 && heightMax < value.format.height) {
                                        data = data.copy(videoUrl = value.url, isVideoHls = value.format.isHlsContent, isVideoDash = value.format.isDashContainer)
                                    }
                                }
                            }
                            videoMeta?.let {
                                meta->
                                data = data.copy(title = meta.title, thumbnail = meta.hqImageUrl,
                                author = meta.author, isLivestream = meta.isLiveStream, duration = meta.videoLength)
                            }
                            data
                        } else {
                            null
                        }
                        continuation.resume(result)
                    }
                    extractor.extract(url)
                }
            L.e("data = $item")
            item
        }
        _youtubeList.postValue(newList)
    }

    fun getLists() {
        viewModelScope.launch(Dispatchers.IO) {
            parseList()
        }
    }

}