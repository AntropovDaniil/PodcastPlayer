package com.example.podcastplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.podcastplayer.repository.ItunesRepo
import com.example.podcastplayer.service.PodcastResponse
import com.example.podcastplayer.util.DateUtils

class SearchViewModel(application: Application): AndroidViewModel(application) {
    var iTunesRepo: ItunesRepo? = null

    data class PodcastSummaryViewData(
        var name: String? = "",
        var lastUpdated: String? = "",
        var imageUrl: String? = "",
        var feedUrl: String? = "")

    private fun itunesPodcastToPodcastSummaryView(itunesPodcast: PodcastResponse.ITunesPodcast): PodcastSummaryViewData{

        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
            DateUtils.jsonDateToShortDate(itunesPodcast.releaseDate),
            itunesPodcast.artworkUrl30,
            itunesPodcast.feedUrl)
    }

    fun searchPodcast(term: String, callback: (List<PodcastSummaryViewData>) -> Unit) {
        iTunesRepo?.searchByTerm(term) {
            results ->
            if (results == null)  callback(emptyList())
            else {
                val searchViews = results.map { podcast ->
                    itunesPodcastToPodcastSummaryView(podcast)
                }
                callback(searchViews)
            }
        }

    }
}

