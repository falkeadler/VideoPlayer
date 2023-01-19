package falkeadler.library.youtubedataextractor

import kotlinx.serialization.Serializable
@Serializable
data class DataRange(
    val start: Long = -1,
    val end: Long = -1)

@Serializable
data class ColorInformation(
    val primaries: String = "",
    val transferCharacteristics: String = "",
    val matrixCoefficients: String = "")

@Serializable
data class FormatInformation(
    val itag: Int,
    val url: String = "",
    val mimeType:String,
    val bitrate: Long,
    val width: Int = -1,
    val height: Int = -1,
    val initRange: DataRange = DataRange(),
    val indexRange: DataRange = DataRange(),
    val lastModified: String = "",
    val contentLength: String = "0",
    val quality: String = "",
    val fps: Int = -1,
    val qualityLabel: String = "",
    val projectionType: String = "",
    val averageBitrate: Long = -1,
    val colorInfo: ColorInformation = ColorInformation(),
    val approxDurationMs: String = "0",
    val highReplication: Boolean = false,
    val audioQuality: String = "",
    val audioSampleRate: String = "",
    val audioChannels: Long = -1,
    val loudnessDb: Double = 0.0,
    val signatureCipher: String = "")

@Serializable
data class ThumbnailItem(
    val url: String = "",
    val width: Int = -1,
    val height: Int = -1)

@Serializable
data class Thumbnail(val thumbnails: List<ThumbnailItem> = listOf())

@Serializable
data class VideoDetails(
    val videoId:String,
    val title: String,
    val lengthSeconds: String,
    val keywords: List<String> = listOf(),
    val channelId: String,
    val isOwnerViewing: Boolean = false,
    val shortDescription: String = "",
    val isCrawlable: Boolean = false,
    val thumbnail: Thumbnail,
    val allowRatings: Boolean = false,
    val viewCount: String = "",
    val author: String,
    val isPrivate: Boolean = false,
    val isUnpluggedCorpus: Boolean =  false,
    val isLiveContent: Boolean = false)

@Serializable
data class StreamingData(
    val formats: List<FormatInformation> = listOf(),
    val expiresInSeconds: String = "",
    val adaptiveFormats: List<FormatInformation>)

@Serializable
data class YTPlayerResponse(
    val videoDetails: VideoDetails,
    val streamingData: StreamingData)

@Serializable
data class ItemVideo(
    val itag: Int,
    val url: String,
    val width: Int,
    val height: Int,
    val averageBitrate: Long,
    val approxDurationMs: Long)

@Serializable
data class ItemAudio(
    val itag: Int,
    val url: String,
    val audioSampleRate: String,
    val audioChannels: Long,
    val loudnessDb: Double,
    val approxDurationMs: Long, )

@Serializable
data class ItemEither(
    val itag: Int,
    val url: String,
    val audioSampleRate: String,
    val audioChannels: Long,
    val loudnessDb: Double,
    val approxDurationMs: Long,
    val width: Int,
    val height: Int,
    val averageBitrate: Long)

@Serializable
data class YouTubeData(
    val videoId: String = "",
    val expiresInSeconds: Long = 0,
    val title: String = "",
    val lengthSeconds: Long = 0,
    val shortDescription: String = "",
    val thumbnail: ThumbnailItem = ThumbnailItem(),
    val author: String = "",
    val isLiveContent: Boolean = false,
    val videos: List<ItemVideo> = listOf(),
    val audios: List<ItemAudio> = listOf(),
    val oldSchool: List<ItemEither> = listOf(),
    val hlsStream: List<ItemEither> = listOf())

@Serializable
data class CacheFunctionData(
    val functionName: String,
    val functionCode: String)