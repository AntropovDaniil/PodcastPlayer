package com.example.podcastplayer.repository

import com.example.podcastplayer.service.ItunesService
import com.example.podcastplayer.service.PodcastResponse
import retrofit2.Call
import retrofit2.Response

class ItunesRepo(private val itunesService: ItunesService) {

    fun searchByTerm(term: String, callBack: (List<PodcastResponse.ITunesPodcast>?) -> Unit){
        val podcastCall = itunesService.searchPodcastByTerm(term)
        podcastCall.enqueue( object : retrofit2.Callback<PodcastResponse> {
            override fun onFailure(call: Call<PodcastResponse>?, t: Throwable?){
                callBack(null)
            }

            override fun onResponse(call: Call<PodcastResponse>?, response: Response<PodcastResponse>?){
                val body = response?.body()
                callBack(body?.results)
            }
        })
    }
}