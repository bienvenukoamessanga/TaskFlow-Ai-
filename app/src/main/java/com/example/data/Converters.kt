package com.example.data

import androidx.room.TypeConverter
import com.example.model.SubTask
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromStatus(status: TaskStatus?): String {
        return status?.name ?: TaskStatus.TODO.name
    }

    @TypeConverter
    fun toStatus(value: String?): TaskStatus {
        return try {
            if (value != null) TaskStatus.valueOf(value) else TaskStatus.TODO
        } catch (_: Exception) {
            TaskStatus.TODO
        }
    }

    @TypeConverter
    fun fromPriority(priority: TaskPriority?): String {
        return priority?.name ?: TaskPriority.MEDIUM.name
    }

    @TypeConverter
    fun toPriority(value: String?): TaskPriority {
        return try {
            if (value != null) TaskPriority.valueOf(value) else TaskPriority.MEDIUM
        } catch (_: Exception) {
            TaskPriority.MEDIUM
        }
    }

    @TypeConverter
    fun fromSubtaskList(subtasks: List<SubTask>?): String {
        if (subtasks.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (item in subtasks) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("isDone", item.isDone)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toSubtaskList(value: String?): List<SubTask> {
        if (value.isNullOrBlank()) return emptyList()
        val list = mutableListOf<SubTask>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val title = obj.optString("title", "")
                val isDone = obj.optBoolean("isDone", false)
                list.add(SubTask(id = id, title = title, isDone = isDone))
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }
}
