package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val originalVideoUri: String,
    val durationMs: Long,
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val lastEditedTimestamp: Long,
    val createdTimestamp: Long,
    val thumbnailUri: String,
    val projectJson: String
)

@Entity(tableName = "exported_videos")
data class ExportedVideoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fps: Int,
    val fileSize: Long,
    val createdAt: Long,
    val thumbnailUri: String
)
