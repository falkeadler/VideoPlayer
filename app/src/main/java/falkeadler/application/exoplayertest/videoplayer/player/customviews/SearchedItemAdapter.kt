package falkeadler.application.exoplayertest.videoplayer.player.customviews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import falkeadler.application.exoplayertest.videoplayer.data.SearchedItem
import falkeadler.application.exoplayertest.videoplayer.databinding.LoadingItemBinding
import falkeadler.application.exoplayertest.videoplayer.databinding.SearchedItemBinding

class SearchedItemAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val ITEM_TYPE_MOVIE = 0
        const val ITEM_TYPE_LOADING = 1
    }
    inner class ItemViewHolder(private val binding: SearchedItemBinding): ViewHolder(binding.root) {
        fun configure(item: SearchedItem) {
            binding.title.text = item.title
            binding.information.text = item.releaseDate
            binding.root.setOnClickListener {
                cb?.let { it1 -> it1(list[absoluteAdapterPosition].id) }
            }
        }
    }

    inner class LoadingViewHolder(private val binding: LoadingItemBinding): ViewHolder(binding.root) {

    }

    private var list: MutableList<SearchedItem> = mutableListOf()
    val lastIndex: Int
        get() {
            return list.lastIndex
        }

    val lastIsLoading: Boolean
        get() {
            return list.isNotEmpty() && list.last().id == -1
        }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].id != -1) ITEM_TYPE_MOVIE else ITEM_TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_TYPE_MOVIE) {
            ItemViewHolder(SearchedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            LoadingViewHolder(LoadingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.configure(list[position])
        }
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    var cb: ((tmdbId:Int) -> Unit)? = null

    fun setOnClickListener(callback: (Int) -> Unit) {
        cb = callback
    }

    fun addItem(item: SearchedItem): Int {
        return if (item.id == -2) {
            -2
        } else {
            if (item.id == -1 && list.isNotEmpty() && list.last().id == -1) {
                return 0
            }
            if (item.id != -1 && list.isNotEmpty() && list.last().id == -1) {
                list.removeLast()
                notifyItemRemoved(list.lastIndex + 1)
            }
            list.add(item)
            notifyItemInserted(list.lastIndex)
            item.id
        }
    }
}