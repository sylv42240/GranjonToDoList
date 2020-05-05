package com.sylv42240.granjontodolist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter : RecyclerView.Adapter<TaskViewHolder>() {

    private val taskList: MutableList<Task> = mutableListOf()
    lateinit var onTaskClickListener: (Task, Int) -> Unit
    lateinit var onLongTaskClickListener: (Task, Int) -> Unit
    private var sortMode = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val v =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.task_holder, parent, false)
        return TaskViewHolder(v)
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val taskDetail = taskList[position]
        holder.bindData(taskDetail, position, onTaskClickListener, onLongTaskClickListener)
    }

    fun setTaskList(songList: List<Task>, currentSearch: String) {
        this.taskList.clear()
        if (currentSearch.isNotBlank()){
            this.taskList.addAll(songList.filter { it.name.toUpperCase().contains(currentSearch.toUpperCase()) })
        }else{
            this.taskList.addAll(songList)
        }
        when (sortMode) {
            0 -> notifyDataSetChanged()
            1 -> sortListByName()
            2 -> sortListByChecked()
            3 -> sortListByCreation()
            4 -> sortListByUpdate()
        }
    }

    fun addItem(task: Task) {
        this.taskList.add(task)
        notifyItemChanged(this.taskList.lastIndex)
    }

    fun removeAt(position: Int) {
        this.taskList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getTaskAt(position: Int): Task {
        return this.taskList[position]
    }

    fun sortListByName() {
        this.taskList.sortBy { it.name.toUpperCase() }
        notifyDataSetChanged()
        sortMode = 1
    }

    fun sortListByChecked() {
        this.taskList.sortBy { !it.isChecked }
        notifyDataSetChanged()
        sortMode = 2
    }

    fun sortListByCreation() {
        this.taskList.sortByDescending { it.createdAt }
        notifyDataSetChanged()
        sortMode = 3
    }

    fun sortListByUpdate() {
        this.taskList.sortByDescending { it.updatedAt }
        notifyDataSetChanged()
        sortMode = 4
    }

}

