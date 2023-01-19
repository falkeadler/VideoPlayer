package falkeadler.application.exoplayertest.videoplayer.list.customviews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.databinding.ContentItemBinding
import falkeadler.application.exoplayertest.videoplayer.setDurationText
import falkeadler.library.youtubedataextractor.YouTubeData

class YoutubeItemAdapter: RecyclerView.Adapter<YoutubeItemAdapter.YoutubeItemHolder>() {

    private var list = mutableListOf<YouTubeData>()
    private var callback: OnItemClickListener? = null
    val lastIndex: Int
        get() = list.lastIndex
    inner class YoutubeItemHolder(private val binding: ContentItemBinding): ViewHolder(binding.root) {
        fun configure(item: YouTubeData) {
            Glide.with(this.itemView).load(item.thumbnail.url).placeholder(R.drawable.video_icon).into(binding.videoThumbnail)
            binding.title.text = item.title
            binding.smallInformation.text = "${item.author}"
            binding.videoDuration.setDurationText(item.lengthSeconds, true)
            binding.liveIndicator.visibility = if(item.isLiveContent) View.VISIBLE else View.GONE
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

    fun updateList(list: List<YouTubeData>) {
        this.list = list.toMutableList()
        notifyDataSetChanged()
    }

    fun addItem(item: YouTubeData) {
        list.add(item)
        notifyItemInserted(list.lastIndex)
    }

    interface OnItemClickListener {
        fun onItemClick(item: YouTubeData)
    }

    fun setOnItemClickListener(cb: OnItemClickListener) {
        callback = cb
    }

    fun setOnItemClickListener(cb: (YouTubeData) -> Unit) {
        callback = object : OnItemClickListener {
            override fun onItemClick(item: YouTubeData) {
                cb(item)
            }
        }
    }
}