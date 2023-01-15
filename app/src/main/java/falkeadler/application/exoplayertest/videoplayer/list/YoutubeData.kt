package falkeadler.application.exoplayertest.videoplayer.list

data class YoutubeData(
    val videoUrl: String = "",
    val audioUrl: String = "",
    val avUrl: String = "",
    val title: String = "",
    val isLivestream: Boolean = false,
    val isVideoDash: Boolean = false,
    val isVideoHls: Boolean = false,
    val isAudioDash: Boolean = false,
    val isAudioHls: Boolean = false,
    val thumbnail: String = "",
    val author: String = "",
    val duration: Long = 0) : java.io.Serializable