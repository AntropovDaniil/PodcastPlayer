package com.example.podcastplayer.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.example.podcastplayer.model.Episode
import com.example.podcastplayer.model.Podcast

@Dao
interface PodcastDao{

    @Query("SELECT * FROM Podcast ORDER BY FeedTitle")
    fun loadPodcasts(): LiveData<List<Podcast>>

    @Query("SELECT * FROM Episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
    fun loadEpisodes(podcastId: Long): List<Episode>

    @Insert(onConflict = REPLACE)
    fun insertPodcast(podcast: Podcast): Long

    @Insert(onConflict = REPLACE)
    fun insertEpisode(episode: Episode): Long

    @Query("SELECT * FROM Podcast WHERE feedUrl = :url")
    fun loadPodcast(url: String): Podcast?

    @Delete
    fun deletePodcast(podcast: Podcast)

    @Query("SELECT * FROM Podcast ORDER BY FeedTitle")
    fun loadPodcastsStatic(): List<Podcast>
}