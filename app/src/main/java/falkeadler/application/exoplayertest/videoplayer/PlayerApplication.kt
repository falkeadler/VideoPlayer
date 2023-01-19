package falkeadler.application.exoplayertest.videoplayer

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent

class PlayerApplication: Application() {
    override fun onCreate() {
        super.onCreate()
    }
    val youtube_key = "AIzaSyBRUEHcuNquL1dXg0BdPrNIxAahkGmeehg"
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
}