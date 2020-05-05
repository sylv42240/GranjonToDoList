package com.sylv42240.granjontodolist

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BasicGridItem
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.input.input
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TaskAdapter
    private val database = FirebaseDatabase.getInstance()
    private val taskReference = database.getReference("Tasks")
    private var currentSearch = ""
    private val currentList = mutableListOf<Task>()
    private val completeList = mutableListOf<Task>()
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initRecyclerView()
        retrieveData()
        setupActionButton()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar, menu)
        val manager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchItem = menu.findItem(R.id.search_item)
        val searchView = searchItem.actionView as SearchView
        this.searchView = searchView

        searchView.setSearchableInfo(manager.getSearchableInfo(componentName))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    currentSearch = query
                    searchView.clearFocus()
                    setupRecyclerView(currentList)
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return if (newText != null) {
                    currentSearch = newText
                    true
                } else {
                    false
                }
            }

        })
        searchView.setOnCloseListener {
            currentSearch = ""
            setupRecyclerView(completeList)
            return@setOnCloseListener false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val items = listOf(
            BasicGridItem(R.drawable.ic_sort_by_alpha, "Name"),
            BasicGridItem(R.drawable.ic_check_black, "Checked"),
            BasicGridItem(R.drawable.ic_create, "Creation"),
            BasicGridItem(R.drawable.ic_update, "Update")
        )

        if (item.itemId == R.id.main_toolbar_filter_icon) {
            MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                gridItems(items) { _, _, item ->
                    when (item.title) {
                        "Name" -> {
                            adapter.sortListByName()
                        }
                        "Checked" -> {
                            adapter.sortListByChecked()
                        }
                        "Creation" -> {
                            adapter.sortListByCreation()
                        }
                        "Update" -> {
                            adapter.sortListByUpdate()
                        }
                    }
                }
                title(R.string.sort)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        task_recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter()
        adapter.onTaskClickListener = this::onTaskClickListener
        adapter.onLongTaskClickListener = this::onLongTaskClickListener
        task_recycler_view.adapter = adapter
        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val taskToRemove = adapter.getTaskAt(viewHolder.adapterPosition)
                adapter.removeAt(viewHolder.adapterPosition)
                taskReference.child(taskToRemove.firebaseId).removeValue()
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(task_recycler_view)
    }

    private fun retrieveData() {
        taskReference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d("FirebaseError", error.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val taskList = mutableListOf<Task>()
                val taskMap = dataSnapshot.value as? HashMap<*, *>
                taskMap?.map { entry ->
                    val task = entry.value as HashMap<*, *>
                    val id = entry.key as String
                    val name = task["name"] as String
                    val isChecked = task["isChecked"] as Boolean
                    val createdAt = task["createdAt"] as Long
                    val updatedAt = task["updatedAt"] as Long
                    taskList.add(Task(id, name, isChecked, createdAt, updatedAt))
                }
                setupRecyclerView(taskList)
                completeList.clear()
                completeList.addAll(taskList)
            }

        })
    }

    private fun onTaskClickListener(task: Task, position: Int) {
        task.isChecked = !task.isChecked
        task.updatedAt = System.currentTimeMillis()
        val updatedTaskMap: MutableMap<String, Any> = HashMap()
        updatedTaskMap["isChecked"] = task.isChecked
        updatedTaskMap["updatedAt"] = task.updatedAt
        taskReference.child(task.firebaseId).updateChildren(updatedTaskMap)
        adapter.notifyItemChanged(position)
    }

    private fun onLongTaskClickListener(task: Task, position: Int) {
        MaterialDialog(this).show {
            input(prefill = task.name) { _, text ->
                task.name = text.toString()
                task.updatedAt = System.currentTimeMillis()
                val updatedTaskMap: MutableMap<String, Any> = HashMap()
                updatedTaskMap["name"] = text.toString()
                updatedTaskMap["updatedAt"] = task.updatedAt
                taskReference.child(task.firebaseId).updateChildren(updatedTaskMap)
                adapter.notifyItemChanged(position)
            }
            title(R.string.modify_title)
            positiveButton(R.string.modify)
        }

    }

    private fun setupRecyclerView(taskList: MutableList<Task>) {
        adapter.setTaskList(taskList, currentSearch)
        currentList.clear()
        currentList.addAll(taskList)
    }

    private fun setupActionButton() {
        add_task_button.setOnClickListener {
            MaterialDialog(this).show {
                input { _, text ->
                    val task = HashMap<String, Any>()
                    task["name"] = text.toString()
                    task["isChecked"] = false
                    task["createdAt"] = System.currentTimeMillis()
                    task["updatedAt"] = System.currentTimeMillis()
                    val taskRef = taskReference.push()
                    taskRef.setValue(task)
                    val id = taskRef.key
                    val name = task["name"] as String
                    val isChecked = task["isChecked"] as Boolean
                    val createdAt = task["createdAt"] as Long
                    val updatedAt = task["updatedAt"] as Long
                    id?.let { it1 -> Task(it1, name, isChecked, createdAt, updatedAt) }
                        ?.let { it2 ->
                            adapter.addItem(it2)
                        }
                }
                positiveButton(R.string.add)
                title(R.string.add_title)
            }
        }
    }


}
