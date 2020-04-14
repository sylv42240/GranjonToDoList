package com.sylv42240.granjontodolist

data class Task(
    val firebaseId: String = "",
    var name: String = "",
    var isChecked: Boolean = false,
    var createdAt: Long = 0,
    var updatedAt: Long = 0
)