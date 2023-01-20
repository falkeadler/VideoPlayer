package falkeadler.application.exoplayertest.videoplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import falkeadler.application.exoplayertest.videoplayer.databinding.ActivityMainBinding
import falkeadler.application.exoplayertest.videoplayer.list.TestFragment
import falkeadler.application.exoplayertest.videoplayer.list.VideoItemFragment
import falkeadler.application.exoplayertest.videoplayer.list.YoutubeItemFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val pagerAdapter: PagerAdapter by lazy {
        PagerAdapter(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            pager.adapter = pagerAdapter
            pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    // 뭐든 하면 됨
                    super.onPageSelected(position)
                }
            })
            pagerAdapter.addFragment(VideoItemFragment())
            pagerAdapter.addFragment(YoutubeItemFragment())
            pagerAdapter.addFragment(TestFragment())
            TabLayoutMediator(tabs, pager) {
                    tab, position ->
                when(position) {
                    0 -> tab.text = "VideoItem"
                    1 -> tab.text = "BANCED????"
                    else -> tab.text = "TEST?"
                }
            }.attach()
        }
        setSupportActionBar(binding.toolbar)
    }
}