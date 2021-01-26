package com.example.podcastplayer.repository

import com.example.podcastplayer.model.Podcast

class PodcastRepository {

    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit){
        callback(Podcast(feedUrl, "No Name", "No description", "No image"))
    }
}