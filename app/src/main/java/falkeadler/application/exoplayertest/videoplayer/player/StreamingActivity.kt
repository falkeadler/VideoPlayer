package falkeadler.application.exoplayertest.videoplayer.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Rational
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.video.VideoSize
import com.google.gson.Gson
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.list.YoutubeData
import falkeadler.application.exoplayertest.videoplayer.player.viewmodel.VideoData

class StreamingActivity : PlayerActivityBase() {

    override fun handleIntent(intent: Intent?) {
        intent?.extras?.getString("YOUTUBEDATA")?.let {

            val data = Gson().fromJson(it, YoutubeData::class.java)
            if (data.title.isNotEmpty()) {
                supportActionBar?.title = data.title
            }
            if (data.audioUrl.isNotEmpty() && data.videoUrl.isNotEmpty()) {
                val audio = buildDataSource(data.audioUrl, data.isAudioHls, data.isAudioDash)
                val video = buildDataSource(data.videoUrl, data.isVideoHls, data.isVideoDash)
                val mergeSource = MergingMediaSource(video, audio)
                player.addMediaSource(mergeSource)
                player.prepare()
            } else {
                player.addMediaSource(buildDataSource(data.avUrl))
                player.prepare()
            }
        } ?: run {
            // 여기선 일단 DATA를 보고 시작하자.
            intent?.data?.let {
                player.addMediaSource(buildDataSource(it))
                player.prepare()
            } ?: run {
                finish()
            }
        }
    }


    private fun buildDataSource(uri: Uri, isHls:Boolean = false, isDash:Boolean = false) : MediaSource {
        return buildDataSource(uri.toString(), isHls, isDash)
    }
    private fun buildDataSource(urlString: String, isHls:Boolean = false, isDash:Boolean = false) : MediaSource {
        val mediaItem = MediaItem.fromUri(urlString)
        val factory = DefaultHttpDataSource.Factory()
        return if (isHls) {
           if (isDash && urlString.endsWith(".mpd")) {
               DashMediaSource.Factory(factory).createMediaSource(mediaItem)

           } else {
               HlsMediaSource.Factory(factory).createMediaSource(mediaItem)
           }
        } else {
            ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem)
        }
    }

    override fun updatePlaybackStateForMediaSession() {

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
        binding.playController.bufferingStateChanged(false)
        if (playbackState == Player.STATE_ENDED) {
            finish()
        }
        if (playbackState == Player.STATE_BUFFERING) {
            L.e("buffering : ${player.bufferedPercentage}%")
            binding.playController.bufferingStateChanged(true)
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
}