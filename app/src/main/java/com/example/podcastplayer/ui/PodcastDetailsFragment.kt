package com.example.podcastplayer.ui

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.podcastplayer.R
import com.example.podcastplayer.adapter.EpisodeListAdapter
import com.example.podcastplayer.databinding.FragmentPodcastDetailsBinding
import com.example.podcastplayer.service.PodcastPlayMediaService
import com.example.podcastplayer.viewmodel.PodcastViewModel
import java.lang.RuntimeException

class PodcastDetailsFragment: Fragment(),EpisodeListAdapter.EpisodeListAdapterListener {

    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var binding: FragmentPodcastDetailsBinding
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private var listener: OnPodcastDetailListener? = null
    private var menuItem: MenuItem? = null

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null

    companion object{
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPodcastDetailListener){
            listener = context
        }
        else{
            throw RuntimeException(context.toString() +
                    " must implement OnPodcastDetailsListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initMediaBrowser()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPodcastDetailsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        updateControls()
    }

    override fun onStart() {
        super.onStart()
        if (mediaBrowser.isConnected){
            val fragmentActivity = activity as FragmentActivity
            if (MediaControllerCompat.getMediaController(fragmentActivity) == null){
                registerMediaController(mediaBrowser.sessionToken)
            }
        }
        else{
            mediaBrowser.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null){
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity)
                    .unregisterCallback(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
        menuItem = menu.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_feed_action -> {
                podcastViewModel.activePodcastViewData?.feedUrl?.let {
                    if (podcastViewModel.activePodcastViewData?.subscribed == true){
                        listener?.onUnsubscribe()
                    }
                    else{
                        listener?.onSubscribe()
                    }
                }
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)

        if (controller.playbackState != null){
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING){
                controller.transportControls.pause()
            } else{
                startPlaying(episodeViewData)
            }
        } else{
            startPlaying(episodeViewData)
        }
    }

    private fun updateControls(){
        val viewData = podcastViewModel.activePodcastViewData ?: return
        binding.feedTitleTextView.text = viewData.feedTitle
        binding.feedDescTextView.text = viewData.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.imageUrl)
                    .into(binding.feedImageView)
        }
    }

    private fun setupControls(){
        binding.feedDescTextView.movementMethod = ScrollingMovementMethod()
        binding.episodeRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        binding.episodeRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            binding.episodeRecyclerView.context, layoutManager.orientation)
        binding.episodeRecyclerView.addItemDecoration(dividerItemDecoration)
        episodeListAdapter = EpisodeListAdapter(podcastViewModel.activePodcastViewData?.episodes, this)
        binding.episodeRecyclerView.adapter = episodeListAdapter
    }

    private fun updateMenuItem(){
        val viewData = podcastViewModel.activePodcastViewData ?: return

        menuItem?.title = if (viewData.subscribed){
            getString(R.string.unsubscribe)
        }
        else getString(R.string.subscribe)
    }

    private fun registerMediaController(token: MediaSessionCompat.Token){
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)

        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    private fun initMediaBrowser(){
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(fragmentActivity,
        ComponentName(fragmentActivity,
        PodcastPlayMediaService::class.java),
            MediaBrowserCallBacks(),
            null
        )
    }

    private fun startPlaying(episodeViewData: PodcastViewModel.EpisodeViewData){
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)

        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()
        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
            episodeViewData.title)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
            viewData.feedTitle)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
            viewData.imageUrl)

        controller.transportControls.playFromUri(
            Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    interface OnPodcastDetailListener{
        fun onSubscribe()
        fun onUnsubscribe()
    }

    inner class MediaControllerCallback: MediaControllerCompat.Callback(){

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            //TODO
            println("metadata changed to ${metadata?.getString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            //TODO
            println("state changed to $state")
        }
    }

    inner class MediaBrowserCallBacks: MediaBrowserCompat.ConnectionCallback(){

        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            //Todo
            println("onConnected")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            //TODO
            println("onConnectionSuspended")
            //Disable transport controls
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            //TODO
            println("onConnectionFailed")
            //Fatal error handling
        }
    }
}