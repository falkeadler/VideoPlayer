package falkeadler.application.exoplayertest.videoplayer.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import falkeadler.application.exoplayertest.videoplayer.databinding.TestFragmentBinding
import falkeadler.application.exoplayertest.videoplayer.player.PlayerActivity

class TestFragment: Fragment() {

    private var binding: TestFragmentBinding? = null
    private val strHLS = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
    private val strDASH = "https://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube?as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,source,id,as&ip=0.0.0.0&ipbits=0&expire=19000000000&signature=51AF5F39AB0CEC3E5497CD9C900EBFEAECCCB5C7.8506521BFC350652163895D4C26DEE124209AA9E&key=ik0"
    private val strPROG = "https://html5demos.com/assets/dizzy.mp4"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = TestFragmentBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.run {
            hls.setOnClickListener {
                Intent(requireActivity(), PlayerActivity::class.java).let {
                    it.data = Uri.parse(strHLS)
                    startActivity(it)
                }
            }
            dash.setOnClickListener {
                Intent(requireActivity(), PlayerActivity::class.java).let {
                    it.data = Uri.parse(strDASH)
                    startActivity(it)
                }
            }
            progressive.setOnClickListener {
                Intent(requireActivity(), PlayerActivity::class.java).let {
                    it.data = Uri.parse(strPROG)
                    startActivity(it)
                }
            }
        }
    }
}