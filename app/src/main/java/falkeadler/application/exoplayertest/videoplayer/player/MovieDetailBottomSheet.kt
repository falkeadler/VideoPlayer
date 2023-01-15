package falkeadler.application.exoplayertest.videoplayer.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.drawee.generic.RoundingParams
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.databinding.GenreTextviewBinding
import falkeadler.application.exoplayertest.videoplayer.databinding.MovieDetailLayoutBinding
import falkeadler.application.exoplayertest.videoplayer.player.customviews.SearchedItemAdapter
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.MovieDetailViewModel
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.VideoData
import falkeadler.application.exoplayertest.videoplayer.setRuntimeText


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
            viewModel.buildedPosterPath.observe(viewLifecycleOwner) {
                binding.loading.visibility = View.GONE
                binding.listMovies.visibility = View.GONE
                binding.contents.visibility = View.VISIBLE
                binding.poster.setImageRequest(ImageRequest.fromUri(it))
            }
            viewModel.movieItem.observe(viewLifecycleOwner) {
                binding.loading.visibility = View.GONE
                binding.listMovies.visibility = View.GONE
                binding.contents.visibility = View.VISIBLE
                binding.title.text = it.title
                binding.infoEtc.text = it.overview
                binding.runtime.setRuntimeText(it.runtime)
                binding.year.text = it.releaseDate

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
            }
            binding.closeBtn.setOnClickListener { dismiss() }
            viewModel.searchedItemList.observe(viewLifecycleOwner) {
                if (it.size == 1) {
                    // 바로 쿼리 때림
                    viewModel.getDetail(it.first().id)
                } else if (it.size > 1) {
                    // 리스트 보여줌
                    binding.contents.visibility = View.GONE
                    binding.loading.visibility = View.GONE
                    binding.listMovies.visibility = View.VISIBLE
                    itemAdapter.list = it
                    itemAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "The MovieDB 에 정보가 없어요", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }

            binding.listMovies.adapter = itemAdapter
            binding.listMovies.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

            binding.loading.visibility = View.VISIBLE
            binding.listMovies.visibility = View.GONE
            binding.contents.visibility = View.GONE
            val cornerRadius = 20.0f
            val roundParams =  RoundingParams.fromCornersRadius(cornerRadius)
            binding.poster.hierarchy.roundingParams = roundParams

        }
        viewModel.queryByTitle(data)
    }
    

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}