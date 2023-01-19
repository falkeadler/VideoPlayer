package falkeadler.application.exoplayertest.videoplayer.list

import android.Manifest
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.databinding.DataBindingUtil
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.databinding.FragmentVideoItemBinding
import falkeadler.application.exoplayertest.videoplayer.list.customviews.VideoItemCursorAdapter
import falkeadler.application.exoplayertest.videoplayer.player.LocalPlayerActivity


class VideoItemFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private val loaderId = "falkeadler.application.exoplayertest.videoplayer.list.VideoItemFragment".hashCode()
    private var binding: FragmentVideoItemBinding? = null
    private val videoAdapter = VideoItemCursorAdapter(null)
    private val linearManager: LinearLayoutManager by lazy {
        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoItemBinding.inflate(LayoutInflater.from(requireContext()), container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.let {
            it.itemList.adapter = videoAdapter
            it.itemList.layoutManager = linearManager
            videoAdapter.setOnItemClickListener {
                adapterPosition ->
                val uri = videoAdapter.getUri(adapterPosition)
                val bucketId = videoAdapter.getBucketId(adapterPosition)
                Intent(requireActivity(), LocalPlayerActivity::class.java).run {
                    data = uri
                    putExtra(MediaStore.Video.VideoColumns.BUCKET_ID, bucketId)
                    startActivity(this)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // permission
        when {
            permissionGranted() -> {
                LoaderManager.getInstance(this).initLoader(loaderId, null, this)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                L.e("you should grant permission")
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                L.e("you should grant permission")
            }
            else -> {
                permissionRequester.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        L.e("loader create")
        return CursorLoader(requireContext(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        L.e("loader load finish ${data?.count}")
        videoAdapter.swapCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
    }

    private fun permissionGranted(): Boolean {
       return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED
               && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED
    }

    private val permissionRequester = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        LoaderManager.getInstance(this).initLoader(loaderId, null, this)
    }
}