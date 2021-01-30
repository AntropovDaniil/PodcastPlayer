package com.example.podcastplayer.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.podcastplayer.R
import com.example.podcastplayer.databinding.FragmentEpisodePlayerBinding
import com.example.podcastplayer.service.PodcastPlayMediaService
import com.example.podcastplayer.service.PodplayMediaCallback.Companion.CMD_CHANGESPEED
import com.example.podcastplayer.service.PodplayMediaCallback.Companion.CMD_EXTRA_SPEED
import android.text.format.DateUtils
import android.view.SurfaceHolder
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.podcastplayer.service.PodplayMediaCallback
import com.example.podcastplayer.util.HtmlUtils
import com.example.podcastplayer.viewmodel.PodcastViewModel

class EpisodePlayerFragment: Fragment() {

    private lateinit var binding: FragmentEpisodePlayerBinding
    private val podcastViewModel: PodcastViewModel by activityViewModels()

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private var playerSpeed:Float = 1.0f
    private var episodeDuration: Long = 0
    private var draggingScrubber: Boolean = false
    private var progressAnimator: ValueAnimator? = null

    private var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playOnPrepare: Boolean = false
    private var isVideo: Boolean = false

    companion object{
        fun newInstance(): EpisodePlayerFragment{
            return EpisodePlayerFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            isVideo = podcastViewModel.activeEpisodeViewData?.isVideo ?: false
        } else{
            isVideo = false
        }

        if (!isVideo) {
            initMediaBrowser()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEpisodePlayerBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        if (isVideo){
            initMediaSession()
            initVideoPlayer()
        }
        updateControls()
    }

    override fun onStart() {
        super.onStart()
        if (!isVideo) {
            if (mediaBrowser.isConnected) {
                val fragmentActivity = activity as FragmentActivity
                if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                    registerMediaController(mediaBrowser.sessionToken)
                }
                updateControlsFromController()
            } else {
                mediaBrowser.connect()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null){
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity)
                        .unregisterCallback(it)
            }
        }
        if (isVideo){
            mediaPlayer?.setDisplay(null)
        }

        if (!fragmentActivity.isChangingConfigurations){
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun updateControls(){

        binding.episodeTitleTextView.text = podcastViewModel.activeEpisodeViewData?.title

        val htmlDesc = podcastViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)
        binding.episodeDescTextView.text = descSpan

        binding.episodeDescTextView.movementMethod = ScrollingMovementMethod()

        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity)
                .load(podcastViewModel.activePodcastViewData?.imageUrl)
                .into(binding.episodeImageView)

        binding.speedButton.text = "${playerSpeed}x"

        mediaPlayer?.let { updateControlsFromController()}
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

    private fun initMediaBrowser(){
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(fragmentActivity,
                ComponentName(fragmentActivity,
                        PodcastPlayMediaService::class.java),
                MediaBrowserCallBacks(),
                null
        )
    }

    private fun registerMediaController(token: MediaSessionCompat.Token){
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)

        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    private fun togglePlayPause(){
        playOnPrepare = true
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null){
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING){
                controller.transportControls.pause()
            } else{
                podcastViewModel.activeEpisodeViewData?.let {
                    startPlaying(it)
                }
            }
        } else {
            podcastViewModel.activeEpisodeViewData?.let {
                startPlaying(it)
            }
        }
    }

    private fun setupControls(){
        binding.playToggleButton.setOnClickListener {
            togglePlayPause()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            binding.speedButton.setOnClickListener {
                changeSpeed()
            }
        } else{
            binding.speedButton.visibility = View.INVISIBLE
        }

        binding.forwardButton.setOnClickListener {
            seekBy(30)
        }

        binding.replayButton.setOnClickListener {
            seekBy(-10)
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.currentTimeTextView.text = DateUtils.formatElapsedTime((progress/1000).toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                draggingScrubber = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                draggingScrubber = false

                val fragmentActivity = activity as FragmentActivity
                val controller = MediaControllerCompat.getMediaController(fragmentActivity)

                if (controller.playbackState != null){
                    controller.transportControls.seekTo(binding.seekBar.progress.toLong())
                } else{
                    binding.seekBar.progress = 0
                }
            }
        })
    }

    private fun handleStateChange(state: Int, position: Long, speed: Float){
        progressAnimator?.let {
            it.cancel()
            progressAnimator = null
        }
        val isPLaying = state == PlaybackStateCompat.STATE_PLAYING
        binding.playToggleButton.isActivated = isPLaying

        val progress = position.toInt()
        binding.seekBar.progress = progress
        binding.speedButton.text = "${playerSpeed}x"

        if (isPLaying){
            if (isVideo){
                setupViedoUI()
            }
            animateScrubber(progress, speed)
        }
    }

    private fun changeSpeed(){
        playerSpeed += 0.25f
        if (playerSpeed > 2.0f){
            playerSpeed = 0.75f
        }

        val bundle = Bundle()
        bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(CMD_CHANGESPEED, bundle, null)

        binding.speedButton.text = "${playerSpeed}x"
    }

    private fun seekBy(seconds: Int){
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val newPosition = controller.playbackState.position + seconds*1000
        controller.transportControls.seekTo(newPosition)
    }

    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat){
        episodeDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        binding.endTimeTextView.text = DateUtils.formatElapsedTime(episodeDuration/1000)
        binding.seekBar.max = episodeDuration.toInt()
    }

    private fun animateScrubber(progress: Int, speed: Float){
        val timeRemaining = ((episodeDuration - progress) / speed).toInt()

        if (timeRemaining > 0){
            return
        }
        progressAnimator = ValueAnimator.ofInt(
                progress, episodeDuration.toInt()
        )
        progressAnimator?.let { animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber){
                    animator.cancel()
                }
                else{
                    binding.seekBar.progress = animator.animatedValue as Int
                }
            }
            animator.start()
        }
    }

    private fun updateControlsFromController(){
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)

        if (controller != null){
            val metadata = controller.metadata
            if (metadata != null){
                handleStateChange(controller.playbackState.state,
                controller.playbackState.position, playerSpeed)

                updateControlsFromMetadata(controller.metadata)
            }
        }
    }

    private fun initMediaSession(){
        if (mediaSession == null){
            mediaSession = MediaSessionCompat(activity as Context, "EpisodePlayerFragment")

            mediaSession?.setMediaButtonReceiver(null)
        }
        registerMediaController(mediaSession!!.sessionToken)
    }

    private fun setSurfaceSize(){
        val mediaPlayer = mediaPlayer ?: return

        val videoWidth = mediaPlayer.videoWidth
        val videoHeight = mediaPlayer.videoHeight

        val parent = binding.videoSurfaceView.parent as View
        val containerWidth = parent.width
        val containerHeight = parent.height

        val layoutAspectRatio = containerWidth.toFloat() / containerHeight
        val videoAspectRatio = videoWidth.toFloat() / videoHeight
        val layoutParams = binding.videoSurfaceView.layoutParams

        if (videoAspectRatio > layoutAspectRatio){
            layoutParams.height = (containerWidth / videoAspectRatio).toInt()
        } else{
            layoutParams.width = (containerHeight * videoAspectRatio).toInt()
        }
        binding.videoSurfaceView.layoutParams = layoutParams
    }

    private fun initMediaPlayer(){
        if (mediaPlayer == null){
            mediaPlayer = MediaPlayer()
            mediaPlayer?.let{
                it.setAudioStreamType(AudioManager.STREAM_MUSIC)
                it.setDataSource(podcastViewModel.activeEpisodeViewData?.mediaUrl)

                it.setOnPreparedListener {
                    val fragmentActivity = activity as FragmentActivity
                    val episodeMediaCallback = PodplayMediaCallback(
                            fragmentActivity, mediaSession!!, it)
                    mediaSession!!.setCallback(episodeMediaCallback)

                    setSurfaceSize()
                }

                if (playOnPrepare){
                    togglePlayPause()
                }
                it.prepareAsync()
            }
        } else{
            setSurfaceSize()
        }
    }

    private  fun initVideoPlayer(){
        binding.videoSurfaceView.visibility = View.VISIBLE
        val surfaceHolder = binding.videoSurfaceView.holder

        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initMediaPlayer()
                mediaPlayer?.setDisplay(holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                TODO("Not yet implemented")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun setupViedoUI(){
        binding.episodeDescTextView.visibility = View.INVISIBLE
        binding.headerView.visibility = View.INVISIBLE
        val activity = activity as AppCompatActivity
        activity.supportActionBar?.hide()
        binding.playerControls.setBackgroundColor(Color.argb(255/2, 0, 0, 0))
    }


    inner class MediaControllerCallback: MediaControllerCompat.Callback(){

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            metadata?.let { updateControlsFromMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            val state = state ?: return
            handleStateChange(state.state, state.position, state.playbackSpeed)
        }
    }

    inner class MediaBrowserCallBacks: MediaBrowserCompat.ConnectionCallback(){

        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            updateControlsFromController()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
        }
    }
}