
package com.appcloner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListFragment : Fragment() {

    private var isClonedList: Boolean = false
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppAdapter
    private var appList: MutableList&lt;AppInfo&gt; = mutableListOf()

    companion object {
        fun newInstance(isClonedList: Boolean): AppListFragment {
            val fragment = AppListFragment()
            val args = Bundle()
            args.putBoolean("isClonedList", isClonedList)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isClonedList = arguments?.getBoolean("isClonedList") ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        adapter = AppAdapter(
            appList,
            onSaveClick = { app -&gt;
                (activity as? MainActivity)?.startCloneFlow(app)
            },
            onShareClick = { app -&gt;
                (activity as? MainActivity)?.shareApk(app)
            },
            onInstallClick = { app -&gt;
                (activity as? MainActivity)?.installApk(app)
            },
            onDeleteClick = { app -&gt;
                (activity as? MainActivity)?.showDeleteConfirmDialog(app)
            },
            onLaunchClick = { app -&gt;
                (activity as? MainActivity)?.launchApp(app)
            },
            onDetailClick = { app -&gt;
                (activity as? MainActivity)?.showAppDetails(app)
            }
        )
        
        recyclerView.adapter = adapter
    }

    fun updateAppList(newList: List&lt;AppInfo&gt;) {
        appList.clear()
        appList.addAll(newList)
        adapter.notifyDataSetChanged()
    }
}
