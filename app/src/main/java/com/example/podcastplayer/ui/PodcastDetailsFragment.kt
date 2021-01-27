package com.example.podcastplayer.ui

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.podcastplayer.R
import com.example.podcastplayer.adapter.EpisodeListAdapter
import com.example.podcastplayer.databinding.FragmentPodcastDetailsBinding
import com.example.podcastplayer.viewmodel.PodcastViewModel
import java.lang.RuntimeException

class PodcastDetailsFragment: Fragment() {

    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var binding: FragmentPodcastDetailsBinding
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private var listener: OnPodcastDetailListener? = null
    private var menuItem: MenuItem? = null

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
        episodeListAdapter = EpisodeListAdapter(podcastViewModel.activePodcastViewData?.episodes)
        binding.episodeRecyclerView.adapter = episodeListAdapter
    }

    private fun updateMenuItem(){
        val viewData = podcastViewModel.activePodcastViewData ?: return

        menuItem?.title = if (viewData.subscribed){
            getString(R.string.unsubscribe)
        }
        else getString(R.string.subscribe)
    }

    interface OnPodcastDetailListener{
        fun onSubscribe()
        fun onUnsubscribe()
    }
}