package falkeadler.application.exoplayertest.videoplayer.player.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Video.Media
import androidx.lifecycle.AndroidViewModel
import com.google.android.exoplayer2.MediaItem
import falkeadler.application.exoplayertest.videoplayer.PlayerApplication

class VideoData(val title: String, val duration: Long, val mediaId: String, val width: Int, val height: Int)

class PlayerViewModel(application: Application): AndroidViewModel(application) {
    @SuppressLint("Range", "SuspiciousIndentation")
    fun buildLocalList(uri: Uri, bucketId: String): Pair<List<MediaItem>, Int> {
        val selection = "${MediaStore.Video.VideoColumns.BUCKET_ID}=?"
        val args = arrayOf(bucketId)
        val list: MutableList<MediaItem> =
        getApplication<PlayerApplication>().applicationContext.contentResolver
            .query(Media.EXTERNAL_CONTENT_URI, null, selection, args, null).use {
                it?.let {
                    if(it.count > 0 && it.moveToFirst()) {
                        val innerList = mutableListOf<MediaItem>()
                        do {
                            val id = it.getString(it.getColumnIndex(Media._ID))
                            val data = VideoData(
                                it.getString(it.getColumnIndex(Media.TITLE)),
                                it.getLong(it.getColumnIndex(Media.DURATION)),
                                id,
                                it.getInt(it.getColumnIndex(Media.WIDTH)),
                                it.getInt(it.getColumnIndex(Media.HEIGHT)))
                            val item = MediaItem.Builder().setUri(Uri.withAppendedPath(Media.EXTERNAL_CONTENT_URI, id))
                                .setTag(data)
                                .setMediaId(id)
                                .build()
                                innerList.add(item)
                        } while(it.moveToNext())
                        innerList
                    } else {
                        mutableListOf()
                    }
                } ?: mutableListOf()
            }
        var index = if (list.isNotEmpty()) {
            list.indexOf(list.find { it.mediaId == uri.lastPathSegment })
        } else {
            -1
        }
        return Pair(list, index)
    }

    @SuppressLint("Range")
    fun queryMediaData(uri: Uri): VideoData {
        return getApplication<PlayerApplication>().applicationContext.contentResolver.query(
            uri, null, null, null, null).use {
            it?.let {
                if (it.count >= 1 && it.moveToFirst()) {
                    VideoData(it.getString(it.getColumnIndex(Media.TITLE)),
                        it.getLong(it.getColumnIndex(Media.DURATION)),
                        uri.lastPathSegment!!,
                        it.getInt(it.getColumnIndex(Media.WIDTH)),
                        it.getInt(it.getColumnIndex(Media.HEIGHT)))
                } else {
                    null
                }
            } ?: VideoData("", 0, uri.lastPathSegment!!, 1, 1)
        }
    }
}