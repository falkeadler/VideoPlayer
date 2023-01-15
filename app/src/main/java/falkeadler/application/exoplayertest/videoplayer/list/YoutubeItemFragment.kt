package falkeadler.application.exoplayertest.videoplayer.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import falkeadler.application.exoplayertest.videoplayer.databinding.FragmentYoutubeItemBinding
import falkeadler.application.exoplayertest.videoplayer.list.customviews.YoutubeItemAdapter
import falkeadler.application.exoplayertest.videoplayer.list.viewmodel.YoutubeViewModel
import falkeadler.application.exoplayertest.videoplayer.player.StreamingActivity

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
        binding = FragmentYoutubeItemBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.let {
            binding ->
            binding.bufferingContents.show()
            binding.youtubeList.adapter = youtubeAdapter
            binding.youtubeList.layoutManager = linearLayoutManager
            viewModel.youtubeList.observe(viewLifecycleOwner) {
                binding.bufferingContents.hide()
                youtubeAdapter.updateList(it)
            }
        }
        viewModel.getLists()
    }
}