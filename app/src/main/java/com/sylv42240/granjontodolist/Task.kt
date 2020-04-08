package com.sylv42240.granjontodolist

data class Task(
    val firebaseId: String = "",
    var name: String = "",
    var isChecked: Boolean = false
)