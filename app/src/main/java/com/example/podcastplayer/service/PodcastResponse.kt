package com.example.podcastplayer.service

data class PodcastResponse(
    val resultCount: Int,
    val results: List<ITunesPodcast>){

    data class ITunesPodcast(
        val collectionCensoredName: String,
        val feedUrl: String,
        val artworkUrl100: String,
        val releaseDate: String
    )
}