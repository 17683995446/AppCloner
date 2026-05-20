package com.appcloner

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appcloner.core.model.AppInfo
import com.appcloner.presentation.viewmodel.MainViewModel
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptyDescription: TextView
    private lateinit var emptyActionButton: View
    private lateinit var searchBar: SearchBar
    private lateinit var appAdapter: AppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        setupSearch()
        observeViewModel()
        loadApps()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyState = findViewById(R.id.emptyState)
        emptyTitle = findViewById(R.id.emptyTitle)
        emptyDescription = findViewById(R.id.emptyDescription)
        emptyActionButton = findViewById(R.id.emptyActionButton)
        searchBar = findViewById(R.id.searchBar)

        appAdapter = AppAdapter(
            onAppClick = { app -> showAppDetails(app) },
            onCloneClick = { app -> cloneApp(app) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
        }

        emptyActionButton.setOnClickListener {
            loadApps()
        }
    }

    private fun setupSearch() {
        searchBar.setOnClickListener {
            searchBar.clearText()
            searchBar.expand()
        }

        searchBar.addTransitionListener(object : SearchBar.TransitionListener {
            override fun onTransitionStart(searchBar: SearchBar) {}
            override fun onTransitionEnd(searchBar: SearchBar) {}
        })

        searchBar.addOnEditTextChangeListener { text ->
            if (text.isNullOrBlank()) {
                viewModel.clearSearch()
            } else {
                viewModel.searchApps(text.toString())
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is MainViewModel.UiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        emptyState.visibility = View.GONE
                    }
                    is MainViewModel.UiState.Success -> {
                        progressBar.visibility = View.GONE
                        if (state.apps.isEmpty()) {
                            recyclerView.visibility = View.GONE
                            emptyState.visibility = View.VISIBLE
                            emptyTitle.text = getString(R.string.no_apps_found)
                            emptyDescription.text = getString(R.string.no_apps_description)
                        } else {
                            recyclerView.visibility = View.VISIBLE
                            emptyState.visibility = View.GONE
                            appAdapter.submitList(state.apps)
                        }
                    }
                    is MainViewModel.UiState.Error -> {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                        Timber.e(state.message)
                    }
                }
            }
        }
    }

    private fun loadApps() {
        viewModel.loadInstalledApps()
    }

    private fun showAppDetails(app: AppInfo) {
        Toast.makeText(this, "应用: ${app.appName}", Toast.LENGTH_SHORT).show()
    }

    private fun cloneApp(app: AppInfo) {
        viewModel.cloneApp(app)
        Toast.makeText(this, "开始克隆: ${app.appName}", Toast.LENGTH_SHORT).show()
    }
}