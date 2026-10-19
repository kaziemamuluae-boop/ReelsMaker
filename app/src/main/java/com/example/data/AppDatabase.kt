package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ProjectEditState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Database(
    entities = [ProjectEntity::class, ExportedVideoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun exportedVideoDao(): ExportedVideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reels_maker_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ProjectRepository(private val database: AppDatabase) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(ProjectEditState::class.java)

    val allProjects: Flow<List<ProjectEntity>> = database.projectDao().getAllProjects()
    val allExportedVideos: Flow<List<ExportedVideoEntity>> = database.exportedVideoDao().getAllExportedVideos()

    suspend fun saveProject(state: ProjectEditState) {
        val json = adapter.toJson(state)
        val entity = ProjectEntity(
            id = state.projectId,
            name = state.projectName,
            originalVideoUri = state.originalVideoUri,
            durationMs = state.videoMetadata.durationMs,
            resolutionWidth = state.videoMetadata.width,
            resolutionHeight = state.videoMetadata.height,
            lastEditedTimestamp = System.currentTimeMillis(),
            createdTimestamp = state.createdAt,
            thumbnailUri = state.thumbnailUri,
            projectJson = json
        )
        database.projectDao().insertProject(entity)
    }

    suspend fun loadProject(projectId: String): ProjectEditState? {
        val entity = database.projectDao().getProjectById(projectId) ?: return null
        return try {
            adapter.fromJson(entity.projectJson)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteProject(projectId: String) {
        database.projectDao().deleteProject(projectId)
    }

    suspend fun duplicateProject(projectId: String): String? {
        val existing = loadProject(projectId) ?: return null
        val newId = java.util.UUID.randomUUID().toString()
        val duplicated = existing.copy(
            projectId = newId,
            projectName = "${existing.projectName} (Copy)",
            createdAt = System.currentTimeMillis(),
            lastEditedAt = System.currentTimeMillis()
        )
        saveProject(duplicated)
        return newId
    }

    suspend fun saveExportedVideo(video: ExportedVideoEntity) {
        database.exportedVideoDao().insertExportedVideo(video)
    }

    suspend fun renameExportedVideo(id: String, newTitle: String) {
        database.exportedVideoDao().renameExportedVideo(id, newTitle)
    }

    suspend fun deleteExportedVideo(id: String) {
        database.exportedVideoDao().deleteExportedVideo(id)
    }
}
