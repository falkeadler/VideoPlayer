package falkeadler.application.exoplayertest.videoplayer.player.customviews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import falkeadler.application.exoplayertest.videoplayer.databinding.SearchedItemBinding
import falkeadler.application.exoplayertest.videoplayer.services.SearchedItem

class SearchedItemAdapter: RecyclerView.Adapter<SearchedItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(private val binding: SearchedItemBinding): ViewHolder(binding.root) {
        fun configure(item: SearchedItem) {
            binding.title.text = item.title
            binding.information.text = item.releaseDate
            binding.root.setOnClickListener {
                cb?.let { it1 -> it1(list[absoluteAdapterPosition].id) }
            }
        }
    }

    var list: List<SearchedItem> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(SearchedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.configure(list[position])
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    var cb: ((tmdbId:Int) -> Unit)? = null

    fun setOnClickListener(callback: (Int) -> Unit) {
        cb = callback
    }
}