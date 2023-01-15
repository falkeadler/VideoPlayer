package falkeadler.application.exoplayertest.videoplayer.list.customviews

import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.provider.MediaStore
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.core.ImagePipeline
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.VideoThumbnailExtractor
import falkeadler.application.exoplayertest.videoplayer.databinding.ContentItemBinding
import falkeadler.application.exoplayertest.videoplayer.setDurationText

import java.io.File


class VideoItemCursorAdapter(private var currentCursor: Cursor?): RecyclerView.Adapter<VideoItemCursorAdapter.VideoItem>() {
    private val dataSetObserver = NotifyDataSetObserver()
    private var listener: OnItemClickListener? = null

    init {
        VideoThumbnailExtractor.setOnThumbnailExtractedListener { id, position ->
            currentCursor?.let {
                if (it.count > position) {
                    notifyItemChanged(position)
                }
            }
        }
    }

    inner class VideoItem(private val binding: ContentItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun configure(position: Int) {
            currentCursor?.let {
                it.moveToPosition(position)
                val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.TITLE))
                val resolution = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.RESOLUTION))
                val duration = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION))
                val id = it.getInt(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns._ID))
                binding.run {
                    this.title.text = title
                    this.videoDuration.setDurationText(duration.toLong())
                    this.smallInformation.text = resolution
                    this.videoThumbnail.hierarchy.setPlaceholderImage(R.drawable.video_icon)
                    this.description.visibility = View.GONE
                    if(VideoThumbnailExtractor.contains(id)) {
                        //??? 이걸 어떻게 해볼까......
                        this.videoThumbnail.setImageBitmap(VideoThumbnailExtractor.thumbnailMap[id])
                    } else {
                        val file = File(it.getString(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DATA)))
                        VideoThumbnailExtractor.extractThumbnail(file, id, absoluteAdapterPosition)
                    }
                    this.pie.visibility = View.VISIBLE
                }
            }
            binding.root.setOnClickListener {
                listener?.onClick(absoluteAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoItem {
        return VideoItem(ContentItemBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: VideoItem, position: Int) {
        holder.configure(position)
    }

    override fun getItemCount(): Int {
        if(!dataSetObserver.invalidated) {
            return currentCursor?.count ?: 0
        }
        return 0
    }

    fun getUri(position: Int): Uri? {
        if (position < itemCount) {
            return currentCursor?.let {
                it.moveToPosition(position)
                val id = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns._ID))
                Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
        return null
    }

    fun getBucketId(position: Int): String? {
        if (position < itemCount) {
            return currentCursor?.let {
                it.moveToPosition(position)
                it.getString(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.BUCKET_ID))
            }
        }
        return null
    }

    fun swapCursor(newCursor: Cursor?) {
        val oldCursor = currentCursor
        if (newCursor == currentCursor) {
            return
        }
        // close
        currentCursor?.run {
            dataSetObserver.invalidated = true
            unregisterDataSetObserver(dataSetObserver)
        }
        currentCursor = newCursor
        currentCursor?.run {
            dataSetObserver.invalidated = false
            registerDataSetObserver(dataSetObserver)
            notifyDataSetChanged()
        }
        oldCursor?.close()
    }

    inner class NotifyDataSetObserver: DataSetObserver() {
        var invalidated: Boolean = true
        override fun onChanged() {
            super.onChanged()
            invalidated = false
            notifyDataSetChanged()
        }

        override fun onInvalidated() {
            super.onInvalidated()
            invalidated = true
            notifyDataSetChanged()
        }
    }

    interface OnItemClickListener {
        fun onClick(position: Int)
    }

    fun setOnItemClickListener(callback: OnItemClickListener) {
        listener = callback
    }

    fun setOnItemClickListener(callback: (Int) -> Unit) {
        listener = object : OnItemClickListener {
            override fun onClick(position: Int) {
                callback(position)
            }
        }
    }
}