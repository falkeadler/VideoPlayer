package falkeadler.application.exoplayertest.videoplayer.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.databinding.FragmentYoutubeItemBinding
import falkeadler.application.exoplayertest.videoplayer.list.customviews.YoutubeItemAdapter
import falkeadler.application.exoplayertest.videoplayer.list.viewmodel.YoutubeViewModel
import falkeadler.application.exoplayertest.videoplayer.player.StreamingActivity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

class YoutubeItemFragment: Fragment() {
    private val linearLayoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }
    private var binding : FragmentYoutubeItemBinding? = null
    private lateinit var viewModel: YoutubeViewModel
    private val youtubeAdapter = YoutubeItemAdapter().apply {
        setOnItemClickListener {
            Intent(requireActivity(), StreamingActivity::class.java).run {
                putExtra("YOUTUBEDATA", Gson().toJson(it))
                startActivity(this)
            }
        }
    }
    override fun onAttach(context: Context) {
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))[YoutubeViewModel::class.java]
        super.onAttach(context)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_youtube_item, container, false)
        binding!!.apply {
            currentViewModel = viewModel
            lifecycleOwner = this@YoutubeItemFragment
        }
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.let {
            binding ->
            binding.youtubeList.adapter = youtubeAdapter
            binding.youtubeList.layoutManager = linearLayoutManager
            binding.youtubeList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (!viewModel.loadingStateFlow.value && linearLayoutManager.findLastVisibleItemPosition() == youtubeAdapter.lastIndex) {
                        // search 가능상태고 라스트 인덱스가 나왔는데 스크롤되었으면.
                        viewModel.request()
                    }
                }
            })
            lifecycleScope.launchWhenStarted {
                viewModel.youtubeItemFlow.collect() {
                    youtubeAdapter.addItem(it)
                }
            }
            lifecycleScope.launchWhenStarted {
                viewModel.loadingStateFlow.collectLatest {
                    L.e("[SEARCH]STATECHANGED = $it")
                    if (it) {
                        // loading
                        binding.searchField.endIconDrawable = requireContext().getDrawable(R.drawable.working_in)
                        binding.searchField.setEndIconActivated(false)
                        binding.searchField.setEndIconOnClickListener(null)
                    } else {
                        binding.searchField.endIconDrawable = requireContext().getDrawable(R.drawable.search_drawable)
                        binding.searchField.setEndIconActivated(true)
                        binding.searchField.setEndIconOnClickListener {
                            viewModel.request()
                        }
                        // search
                    }
                }
            }
            binding.searchField.setEndIconOnClickListener {
                viewModel.request()
            }
        }
        //viewModel.getLists()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopRequest()
    }
}