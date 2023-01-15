package falkeadler.application.exoplayertest.videoplayer.list

import android.content.Context
import android.util.SparseArray
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile

class YouTubeUrlExtractor(val context: Context): YouTubeExtractor(context) {
    interface OnExtractionListener {
        fun extractionComplete(ytFiles: SparseArray<YtFile>?, videoMeta: VideoMeta?)
    }
    override fun onExtractionComplete(ytFiles: SparseArray<YtFile>?, videoMeta: VideoMeta?) {
        callback?.extractionComplete(ytFiles, videoMeta)
    }
    private var callback: OnExtractionListener? = null

    fun setOnExtractionListener(cb: OnExtractionListener) {
        callback = cb
    }

    fun setOnExtractionListener(cb: (SparseArray<YtFile>?, VideoMeta?) -> Unit) {
        callback = object : OnExtractionListener {
            override fun extractionComplete(ytFiles: SparseArray<YtFile>?, videoMeta: VideoMeta?) {
                cb(ytFiles, videoMeta)
            }
        }
    }
}