package com.example.podcastplayer.ui

import androidx.appcompat.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.podcastplayer.R
import com.example.podcastplayer.adapter.PodcastListAdapter
import com.example.podcastplayer.databinding.ActivityPodcastBinding
import com.example.podcastplayer.db.PodPlayDatabase
import com.example.podcastplayer.repository.ItunesRepo
import com.example.podcastplayer.repository.PodcastRepository
import com.example.podcastplayer.service.FeedService
import com.example.podcastplayer.service.ItunesService
import com.example.podcastplayer.viewmodel.PodcastViewModel
import com.example.podcastplayer.viewmodel.SearchViewModel

class PodcastActivity : AppCompatActivity(),
    PodcastListAdapter.PodcastListAdapterListener,
    PodcastDetailsFragment.OnPodcastDetailListener{

    private lateinit var binding: ActivityPodcastBinding
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    private  val podcastViewModel by viewModels<PodcastViewModel>()

    companion object{
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewModels()
        updateControls()
        setupPodcastListView()
        handleIntent(intent)
        addBackStackListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)
        searchMenuItem = menu.findItem(R.id.search_item)
        searchMenuItem.setOnActionExpandListener(object :
        MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                showSubscribedPodcasts()
                return true
            }
        })
        val searchView = searchMenuItem.actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        if (supportFragmentManager.backStackEntryCount > 0){
            binding.podcastRecyclerView.visibility = View.INVISIBLE
        }

        if (binding.podcastRecyclerView.visibility == View.INVISIBLE){
            searchMenuItem.isVisible = false
        }
        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onSubscribe() {
        podcastViewModel.saveActivePodcast()
        supportFragmentManager.popBackStack()
    }

    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    private fun setupToolbar(){
        setSupportActionBar(binding.toolbar)
    }

    private fun setupViewModels(){
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)
        val rssService = FeedService.instance
        val db = PodPlayDatabase.getInstance(this)
        val podcastDao = db.podcastDao()
        podcastViewModel.podcastRepo = PodcastRepository(rssService, podcastDao)
    }

    private fun performSearch(term: String){
        showProgressBar()
        searchViewModel.searchPodcast(term){ results ->
            hideProgressBar()
            binding.toolbar.title = term
            podcastListAdapter.setSearchData(results)
        }
    }

    private fun handleIntent(intent: Intent){
        if (Intent.ACTION_SEARCH == intent.action){
            val query =  intent.getStringExtra(SearchManager.QUERY) ?: return
            performSearch(query)
        }
    }

    private fun updateControls(){
        binding.podcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        binding.podcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            binding.podcastRecyclerView.context, layoutManager.orientation)

        binding.podcastRecyclerView.addItemDecoration(dividerItemDecoration)
        podcastListAdapter = PodcastListAdapter(null, this, this)
        binding.podcastRecyclerView.adapter = podcastListAdapter
    }

    private fun showProgressBar(){
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar(){
        binding.progressBar.visibility = View.INVISIBLE
    }

    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData){
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        showProgressBar()

        podcastViewModel.getPodcast(podcastSummaryViewData){
            hideProgressBar()
            if (it != null){
                showDetailsFragment()
            } else{
                showError("Error loading feed $feedUrl")
            }
        }
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment{
        var podcastDetailsFragment = supportFragmentManager
                .findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?

        if (podcastDetailsFragment == null){
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }
        return podcastDetailsFragment
    }

    private fun showDetailsFragment(){
        val podcastDetailsFragment = createPodcastDetailsFragment()
        supportFragmentManager.beginTransaction().add(
                R.id.podcastDetailsContainer,
                podcastDetailsFragment, TAG_DETAILS_FRAGMENT)
                .addToBackStack("DetailsFragment")
                .commit()

        binding.podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = true
    }

    private fun showError(message: String){
        AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok_button), null)
                .create()
                .show()
    }

    private fun addBackStackListener(){
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0){
                binding.podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun showSubscribedPodcasts(){
        val podcasts = podcastViewModel.getPodcasts()?.value
        if (podcasts != null){
            binding.toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun setupPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(this, Observer {
            if (it != null){
                showSubscribedPodcasts()
            }
        })
    }
}