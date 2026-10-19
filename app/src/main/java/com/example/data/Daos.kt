package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastEditedTimestamp DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)
}

@Dao
interface ExportedVideoDao {
    @Query("SELECT * FROM exported_videos ORDER BY createdAt DESC")
    fun getAllExportedVideos(): Flow<List<ExportedVideoEntity>>

    @Query("SELECT * FROM exported_videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): ExportedVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExportedVideo(video: ExportedVideoEntity)

    @Query("UPDATE exported_videos SET title = :newTitle WHERE id = :id")
    suspend fun renameExportedVideo(id: String, newTitle: String)

    @Query("DELETE FROM exported_videos WHERE id = :id")
    suspend fun deleteExportedVideo(id: String)
}
