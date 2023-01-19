package falkeadler.application.exoplayertest.videoplayer.player

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.databinding.GenreTextviewBinding
import falkeadler.application.exoplayertest.videoplayer.databinding.MovieDetailLayoutBinding
import falkeadler.application.exoplayertest.videoplayer.player.customviews.SearchedItemAdapter
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.MovieDetailViewModel
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.VideoData
import falkeadler.application.exoplayertest.videoplayer.setRuntimeText
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MovieDetailBottomSheet(private val data: VideoData): BottomSheetDialogFragment() {
    companion object {
        const val TAG = "MovieDetailBottomSheet"
    }
    private var binding: MovieDetailLayoutBinding? = null
    private val viewModel: MovieDetailViewModel by viewModels()
    private val itemAdapter = SearchedItemAdapter().apply {
        setOnClickListener {
            viewModel.getDetail(it)
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = MovieDetailLayoutBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.let {
            binding ->
            viewModel.movieItem.observe(viewLifecycleOwner) {
                binding.listMovies.visibility = View.GONE
                binding.contents.visibility = View.VISIBLE
                binding.title.text = it.title
                binding.infoEtc.text = it.overview
                binding.runtime.setRuntimeText(it.runtime)
                binding.year.text = it.releaseDate
                binding.genres.removeAllViews()
                val linearContentView = LinearLayout(requireContext())
                linearContentView.orientation = LinearLayout.HORIZONTAL
                for (g in it.genres) {
                    linearContentView.addView(
                        GenreTextviewBinding.inflate(
                            layoutInflater,
                            binding.root,
                            false
                        ).root.apply {
                            text = g.name
                        }
                    )
                }
                binding.genres.addView(linearContentView)
                Glide.with(this).load(it.posterPath).transform(RoundedCorners(25)).into(binding.poster)
            }
            binding.closeBtn.setOnClickListener { dismiss() }
            lifecycleScope.launchWhenResumed {
                viewModel.searchedItemFlow.collect(collector = FlowCollector {
                    val type = itemAdapter.addItem(it)
                    if (type == -1) {
                        binding.listMovies.scrollToPosition(itemAdapter.lastIndex)
                    }
                })
            }

            binding.listMovies.adapter = itemAdapter
            binding.listMovies.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

            binding.contents.visibility = View.GONE
            binding.listMovies.addOnScrollListener(object : RecyclerView.OnScrollListener(){
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (!itemAdapter.lastIsLoading) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        if (layoutManager.findLastVisibleItemPosition() == itemAdapter.lastIndex) {
                            L.e("[SCROLL]onScrolled! and load more!!!!!")
                            viewModel.queryByTitle(data, true)
                        }
                    }
                }
            })
        }
        viewModel.queryByTitle(data, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            override fun onBackPressed() {
                binding?.run {
                    if (contents.visibility == View.VISIBLE) {
                        contents.visibility = View.GONE
                        listMovies.visibility = View.VISIBLE
                    } else {
                        super.onBackPressed()
                    }
                } ?: kotlin.run {
                    super.onBackPressed()
                }
            }
        }.apply {
            setCancelable(false)
        }
    }
    

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}