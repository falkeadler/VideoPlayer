package falkeadler.application.exoplayertest.videoplayer.player.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import falkeadler.application.exoplayertest.videoplayer.L
import falkeadler.application.exoplayertest.videoplayer.services.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import kotlin.math.abs

class MovieDetailViewModel: ViewModel() {

    private val _buildedPosterPath = MutableLiveData<String>()
    val buildedPosterPath: LiveData<String> = _buildedPosterPath
    private val _movieItem = MutableLiveData<MovieItem>()
    val movieItem: LiveData<MovieItem> = _movieItem
    private val _errorOnFinish = MutableLiveData<Boolean>(false)
    val errorOnFinish: LiveData<Boolean> = _errorOnFinish
    private val _searchedItemList = MutableLiveData<List<SearchedItem>>()
    val searchedItemList: LiveData<List<SearchedItem>> = _searchedItemList

    fun queryByTitle(data: VideoData) {
        viewModelScope.launch {
            // test
            val searchedMovies = TMDBService.service.searchMovie(URLEncoder.encode(data.title, "utf-8"))
            //val searchedMovies = searchByTitle(data.title)
            searchedMovies.results?.let {
                L.e("[TESTTAG] item count = ${it.size}")
                if (it.isEmpty()) {
                    _searchedItemList.postValue(listOf())
                } else {
                    _searchedItemList.postValue(it.map { item -> item.extract() })
                }
            }
        }
    }

    fun getDetail(tmdbId: Int) {
        viewModelScope.launch {
            val details = TMDBService.service.getMovieDetail(tmdbId.toString())
            details.let {
                val imageConfig = TMDBService.service.apiConfiguration().images
                var index = -1
                var diffMax = Int.MAX_VALUE
                imageConfig.posterSizes.forEachIndexed { idx, sizeStr ->
                    if (sizeStr.startsWith("w") || sizeStr.startsWith("W")) {
                        val diff = abs(sizeStr.substring(1).toInt() - 200)
                        if (diff < diffMax) {
                            index = idx
                        }
                    }
                }
                val sizeStr = if (index == -1) "original" else imageConfig.posterSizes[index]
                val totalPath = "${imageConfig.secureBaseUrl}$sizeStr${it.posterPath}"
                L.e("[IMAGE] totalPath = $totalPath")
                _buildedPosterPath.postValue(totalPath)
                _movieItem.postValue(it.extract())
            }
        }
    }
}
