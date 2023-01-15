package falkeadler.application.exoplayertest.videoplayer.list.customviews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.facebook.imagepipeline.request.ImageRequest
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.databinding.ContentItemBinding
import falkeadler.application.exoplayertest.videoplayer.list.YoutubeData
import falkeadler.application.exoplayertest.videoplayer.setDurationText

class YoutubeItemAdapter: RecyclerView.Adapter<YoutubeItemAdapter.YoutubeItemHolder>() {

    private var list = listOf<YoutubeData>()
    private var callback: OnItemClickListener? = null
    inner class YoutubeItemHolder(private val binding: ContentItemBinding): ViewHolder(binding.root) {
        fun configure(item: YoutubeData) {
            binding.videoThumbnail.hierarchy.setPlaceholderImage(R.drawable.video_icon)
            val replacedUrl = item.thumbnail.replace("http://", "https://")
            binding.videoThumbnail.setImageRequest(ImageRequest.fromUri(replacedUrl))
            binding.title.text = item.title
            binding.smallInformation.text = "${item.author}"
            binding.videoDuration.setDurationText(item.duration, true)
            binding.liveIndicator.visibility = if(item.isLivestream) View.VISIBLE else View.GONE
            binding.root.setOnClickListener {
                callback?.onItemClick(list[absoluteAdapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YoutubeItemHolder {
        return YoutubeItemHolder(ContentItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: YoutubeItemHolder, position: Int) {
        holder.configure(list[position])
    }

    fun updateList(list: List<YoutubeData>) {
        this.list = list
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onItemClick(item: YoutubeData)
    }

    fun setOnItemClickListener(cb: OnItemClickListener) {
        callback = cb
    }

    fun setOnItemClickListener(cb: (YoutubeData) -> Unit) {
        callback = object : OnItemClickListener {
            override fun onItemClick(item: YoutubeData) {
                cb(item)
            }
        }
    }
}