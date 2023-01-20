package falkeadler.application.exoplayertest.videoplayer

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsDataSourceFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource.EventListener
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.MimeTypes
import java.io.File

class PlayerApplication: Application() {
    override fun onCreate() {
        super.onCreate()
    }
    fun launcherToFront() {
        val am = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (task in am.appTasks) {
            val baseIntent = task.taskInfo.baseIntent
            val categories = baseIntent.categories
            if (categories != null && categories.isNotEmpty() && categories.contains(Intent.CATEGORY_LAUNCHER)) {
                task.moveToFront()
                return
            }
        }
    }

    //is for streaming
    class CacheManger private constructor(val context: Context) {
        companion object {
            private var instance: CacheManger? = null
            fun getInstance(ctx: Context): CacheManger {
                return instance ?: synchronized(this) {
                    instance ?: CacheManger(ctx).also {
                        instance = it
                    }
                }
            }
        }
        val simpleCache: SimpleCache by lazy {
            val cacheSize: Long = 1920 * 1080 * 90
            val exoCacheDir = File(context.cacheDir, "exo_cache_dir")
            SimpleCache(exoCacheDir, LeastRecentlyUsedCacheEvictor(cacheSize), StandaloneDatabaseProvider(context))
        }


        fun buildProgressiveMediaSource(sourceUrl: String): ProgressiveMediaSource {
            return buildProgressiveMediaSource(MediaItem.fromUri(sourceUrl))
        }
        fun buildProgressiveMediaSource(sourceItem: MediaItem): ProgressiveMediaSource {
            val upstreamFactory = DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true))
            val cacheFactory = createCacheDataSourceFactory(upstreamFactory)
            return ProgressiveMediaSource.Factory(cacheFactory).createMediaSource(sourceItem)
        }

        fun buildHLSMediaSource(sourceItem: MediaItem): HlsMediaSource {
            val upstreamFactory = DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true))
            val cacheFactory = createCacheDataSourceFactory(upstreamFactory)
            return HlsMediaSource.Factory(cacheFactory).createMediaSource(sourceItem)
        }
        fun buildHLSMediaSource(sourceUrl: String): HlsMediaSource {
            val item = MediaItem.Builder()
                .setUri(Uri.parse(sourceUrl))
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
            return buildHLSMediaSource(item)
        }

        fun buildDASHMediaSource(sourceItem: MediaItem): DashMediaSource {
            val upstreamFactory = DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true))
            val cacheFactory = createCacheDataSourceFactory(upstreamFactory)
            return DashMediaSource.Factory(cacheFactory).createMediaSource(sourceItem)
        }
        fun buildDASHSMediaSource(sourceUrl: String): DashMediaSource {
            val item = MediaItem.Builder()
                .setUri(Uri.parse(sourceUrl))
                .setMimeType(MimeTypes.APPLICATION_MPD)
                .build()
            return buildDASHMediaSource(item)
        }


        fun createMediaSourceFactory():DefaultMediaSourceFactory {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            val cacheDataSourceFactory = createCacheDataSourceFactory(upstreamFactory)
            return DefaultMediaSourceFactory(cacheDataSourceFactory).setLiveMaxOffsetMs(5000)
        }

        fun createCacheDataSourceFactory(upstreamFactory: DataSource.Factory): CacheDataSource.Factory {
            val factory = CacheDataSource.Factory()
            val cacheSink = CacheDataSink.Factory().setCache(simpleCache)
            factory.setCache(simpleCache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setCacheWriteDataSinkFactory(cacheSink)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .setEventListener(object : EventListener {
                    override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {
                        L.e("[CACHEEXO] readed!! cacheSize = $cacheSizeBytes ||| read = $cachedBytesRead")
                    }

                    override fun onCacheIgnored(reason: Int) {
                        L.e("[CACHEEXO] ignored = $reason")
                    }
                })
            return factory
        }
    }
}