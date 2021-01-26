package com.example.podcastplayer.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.podcastplayer.R
import com.example.podcastplayer.databinding.FragmentPodcastDetailsBinding
import com.example.podcastplayer.viewmodel.PodcastViewModel

class PodcastDetailsFragment: Fragment() {

    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var binding: FragmentPodcastDetailsBinding

    companion object{
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
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
        updateControls()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
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
}