package falkeadler.application.exoplayertest.videoplayer.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Rational
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.R
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.VideoData
import kotlinx.coroutines.launch

class LocalPlayerActivity : PlayerActivityBase() {
    private var playOnlyOne = false
    override fun handleIntent(intent: Intent?) {
        intent?.let {
            oldIntent = it
            if (player.isPlaying) {
                player.stop()
                player.clearMediaItems()
            }
            it.data?.let {
                    data ->
                it.getStringExtra(MediaStore.Video.Media.BUCKET_ID)?.let { bucketId ->
                    playOnlyOne = false
                    lifecycleScope.launch {
                        val (list, index) = playerViewModel.buildLocalList(data, bucketId)
                        player.addMediaItems(list)
                        player.seekToDefaultPosition(index)
                        player.prepare()
                    }
                } ?: run {
                    playOnlyOne = true
                    val item = MediaItem.Builder().setUri(data)
                        .setTag(playerViewModel.queryMediaData(data))
                        .setMediaId(data!!.toString()).build()
                    player.addMediaItem(item)
                    player.prepare()
                }
            }
        } ?: finish()
    }

    override fun updatePlaybackStateForMediaSession() {
        if (mediaSession.isActive) {
            val stateBuilder = PlaybackStateCompat.Builder()
            if (player.mediaItemCount == 1) {
                stateBuilder.setActions(MEDIA_ACTION_PLAY_PAUSE)
            } else {
                when (player.currentMediaItemIndex) {
                    0 -> {
                        stateBuilder.setActions(MEDIA_ACION_PLAY_PAUSE_NEXT)
                    }
                    player.mediaItemCount - 1 -> {
                        stateBuilder.setActions(MEDIA_ACION_PLAY_PAUSE_PRVIOUS)
                    }
                    else -> {
                        stateBuilder.setActions(MEDIA_ACTION_ALL)
                    }
                }
            }
            stateBuilder.setState(
                if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                0,
                1.0f
            )
            mediaSession.setPlaybackState(stateBuilder.build())
        }
    }

    override fun buildSessionCallback(): MediaSessionCompat.Callback = object : MediaSessionCompat.Callback () {
        override fun onPause() {
            super.onPause()
            player.pause()
        }

        override fun onPlay() {
            super.onPlay()
            player.playWhenReady = true
        }

        override fun onFastForward() {
            super.onFastForward()
            player.seekForward()
        }

        override fun onRewind() {
            super.onRewind()
            player.seekBack()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            player.seekToNextMediaItem()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            player.seekToPreviousMediaItem()
        }

        override fun onStop() {
            super.onStop()
            player.stop()
            player.playWhenReady = false
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            if (player.currentMediaItemIndex == player.mediaItemCount - 1 || playOnlyOne) {
                finish()
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaItem?.localConfiguration?.tag?.let {
            if (it is VideoData) {
                supportActionBar?.title = it.title
                if (mediaSession.isActive) {
                    val metaData = MediaMetadataCompat.Builder().putString(
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        it.title
                    ).build()
                    mediaSession.setMetadata(metaData)
                }
            }
        }
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        val param = PictureInPictureParams.Builder()
        param.setAspectRatio(Rational(videoSize.width, videoSize.height))
        setPictureInPictureParams(param.build())
    }

    override fun onPlayPauseClicked() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    override fun onFastForwardClicked() {
        player.seekForward()
    }

    override fun onRewindClicked() {
        player.seekBack()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_player, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.searchTmdb -> {
                player.currentMediaItem?.localConfiguration?.tag?.let {
                    if (it is VideoData) {
                        val frag = MovieDetailBottomSheet(it)
                        frag.show(supportFragmentManager, MovieDetailBottomSheet.TAG)
                    }
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        L.e("cur item = ${player.currentMediaItem}")
        //add bookmark...?
    }
}