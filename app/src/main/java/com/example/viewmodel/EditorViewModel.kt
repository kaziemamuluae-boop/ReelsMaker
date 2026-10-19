package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ExportedVideoEntity
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import com.example.media.ExportOptions
import com.example.media.ExportProgress
import com.example.media.VideoExportEngine
import com.example.media.VideoMetadataExtractor
import com.example.media.VideoPlayerManager
import com.example.model.AspectRatioPreset
import com.example.model.ClipSegment
import com.example.model.FitMode
import com.example.model.FontPreset
import com.example.model.FrameLayer
import com.example.model.FrameStyle
import com.example.model.LogoLayer
import com.example.model.LogoPositionPreset
import com.example.model.ProjectEditState
import com.example.model.TextAnimationType
import com.example.model.TextLayer
import com.example.model.VideoMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(AppDatabase.getInstance(application))
    val playerManager = VideoPlayerManager(application, viewModelScope)
    private val exportEngine = VideoExportEngine(application)

    // Current Project Editing State
    private val _projectState = MutableStateFlow(ProjectEditState())
    val projectState: StateFlow<ProjectEditState> = _projectState.asStateFlow()

    // Undo / Redo Stacks
    private val undoStack = mutableListOf<ProjectEditState>()
    private val redoStack = mutableListOf<ProjectEditState>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Selected Active Layer ID for editing
    private val _selectedLayerId = MutableStateFlow<String?>(null)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    // Projects list & Exported Videos list
    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExportedVideos: StateFlow<List<ExportedVideoEntity>> = repository.allExportedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Export state
    private val _exportProgress = MutableStateFlow<ExportProgress?>(null)
    val exportProgress: StateFlow<ExportProgress?> = _exportProgress.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportOptions = MutableStateFlow(ExportOptions())
    val exportOptions: StateFlow<ExportOptions> = _exportOptions.asStateFlow()

    // UI Message Toast/Snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private fun pushStateToUndo(currentState: ProjectEditState) {
        undoStack.add(currentState)
        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previousState = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(_projectState.value)
        _projectState.value = previousState
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
        applyStateToPlayer(previousState)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val nextState = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(_projectState.value)
        _projectState.value = nextState
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()
        applyStateToPlayer(nextState)
    }

    fun initializeNewProjectWithVideo(uri: Uri) {
        viewModelScope.launch {
            val metadata = VideoMetadataExtractor.extractMetadata(getApplication(), uri)
            val thumbnailUri = VideoMetadataExtractor.extractThumbnail(getApplication(), uri, 1000L)

            val newState = ProjectEditState(
                projectId = UUID.randomUUID().toString(),
                projectName = "Reels_${System.currentTimeMillis() % 10000}",
                originalVideoUri = uri.toString(),
                videoMetadata = metadata,
                trimStartMs = 0L,
                trimEndMs = metadata.durationMs,
                aspectRatio = AspectRatioPreset.RATIO_9_16,
                fitMode = FitMode.FIT,
                createdAt = System.currentTimeMillis(),
                lastEditedAt = System.currentTimeMillis(),
                thumbnailUri = thumbnailUri
            )

            undoStack.clear()
            redoStack.clear()
            _canUndo.value = false
            _canRedo.value = false
            _projectState.value = newState
            _selectedLayerId.value = null

            playerManager.loadVideo(uri)
            playerManager.setTrimRange(0L, metadata.durationMs, enableLoop = true)

            // Auto-save initial state
            repository.saveProject(newState)
        }
    }

    fun loadExistingProject(projectId: String) {
        viewModelScope.launch {
            val savedState = repository.loadProject(projectId)
            if (savedState != null) {
                undoStack.clear()
                redoStack.clear()
                _canUndo.value = false
                _canRedo.value = false
                _projectState.value = savedState
                _selectedLayerId.value = null

                val uri = Uri.parse(savedState.originalVideoUri)
                playerManager.loadVideo(uri, savedState.trimStartMs)
                playerManager.setTrimRange(savedState.trimStartMs, savedState.trimEndMs, enableLoop = true)
            }
        }
    }

    fun saveCurrentProject() {
        viewModelScope.launch {
            val current = _projectState.value.copy(lastEditedAt = System.currentTimeMillis())
            _projectState.value = current
            repository.saveProject(current)
            _userMessage.value = "Project saved successfully!"
        }
    }

    fun updateProjectName(newName: String) {
        if (newName.isBlank()) return
        val current = _projectState.value
        pushStateToUndo(current)
        val updated = current.copy(projectName = newName, lastEditedAt = System.currentTimeMillis())
        _projectState.value = updated
        viewModelScope.launch { repository.saveProject(updated) }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            repository.duplicateProject(projectId)
            _userMessage.value = "Project duplicated!"
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            _userMessage.value = "Project deleted."
        }
    }

    // --- TRIM & SPLIT ---
    fun updateTrimRange(startMs: Long, endMs: Long) {
        val current = _projectState.value
        val clampedStart = startMs.coerceIn(0L, current.videoMetadata.durationMs)
        val clampedEnd = endMs.coerceIn(clampedStart + 500L, current.videoMetadata.durationMs)

        pushStateToUndo(current)
        val updated = current.copy(trimStartMs = clampedStart, trimEndMs = clampedEnd)
        _projectState.value = updated
        playerManager.setTrimRange(clampedStart, clampedEnd, enableLoop = true)
        playerManager.seekTo(clampedStart)
    }

    // --- AUTO SPLIT / AUTO CLIP ---
    fun generateAutoSplitClips(intervalSeconds: Int, keepIncomplete: Boolean) {
        val current = _projectState.value
        val duration = current.videoMetadata.durationMs
        if (duration <= 0 || intervalSeconds <= 0) return

        pushStateToUndo(current)
        val intervalMs = intervalSeconds * 1000L
        val segments = mutableListOf<ClipSegment>()

        var start = 0L
        while (start < duration) {
            val end = (start + intervalMs).coerceAtMost(duration)
            if (end - start < intervalMs && !keepIncomplete && segments.isNotEmpty()) {
                break
            }
            segments.add(
                ClipSegment(
                    id = UUID.randomUUID().toString(),
                    startMs = start,
                    endMs = end,
                    isSelected = true
                )
            )
            start = end
        }

        val updated = current.copy(
            autoSplitIntervalSec = intervalSeconds,
            keepIncompleteSegment = keepIncomplete,
            splitSegments = segments
        )
        _projectState.value = updated
        _userMessage.value = "Generated ${segments.size} auto clips!"
    }

    fun selectSegment(segment: ClipSegment) {
        updateTrimRange(segment.startMs, segment.endMs)
    }

    // --- ASPECT RATIO & CANVAS POSITION ---
    fun setAspectRatio(preset: AspectRatioPreset) {
        val current = _projectState.value
        pushStateToUndo(current)
        _projectState.value = current.copy(aspectRatio = preset)
    }

    fun setFitMode(mode: FitMode) {
        val current = _projectState.value
        pushStateToUndo(current)
        _projectState.value = current.copy(fitMode = mode)
    }

    fun updateVideoTransform(panX: Float, panY: Float, zoom: Float, rotation: Float = 0f) {
        val current = _projectState.value
        _projectState.value = current.copy(
            videoOffsetX = panX,
            videoOffsetY = panY,
            videoZoom = zoom.coerceIn(0.5f, 4.0f),
            videoRotation = rotation
        )
    }

    fun toggleAutoReframe() {
        val current = _projectState.value
        pushStateToUndo(current)
        val newState = !current.autoReframeActive
        _projectState.value = current.copy(
            autoReframeActive = newState,
            smartSubjectCenterX = 0.5f // Centers tracking
        )
        _userMessage.value = if (newState) "Smart Auto Reframe Activated" else "Manual Framing Mode"
    }

    // --- TEXT LAYERS ---
    fun addTextLayer(sampleText: String = "Reels Maker") {
        val current = _projectState.value
        pushStateToUndo(current)

        val newLayer = TextLayer(
            id = UUID.randomUUID().toString(),
            text = sampleText,
            fontPreset = FontPreset.SYSTEM_BOLD,
            fontSizeSp = 28f,
            fontColorArgb = 0xFFFFFFFF,
            backgroundColorArgb = 0xCC000000,
            hasOutline = true,
            outlineColorArgb = 0xFFE50914,
            startMs = current.trimStartMs,
            endMs = (current.trimStartMs + 4000L).coerceAtMost(current.trimEndMs)
        )

        val updated = current.copy(textLayers = current.textLayers + newLayer)
        _projectState.value = updated
        _selectedLayerId.value = newLayer.id
    }

    fun updateTextLayer(updatedLayer: TextLayer) {
        val current = _projectState.value
        val list = current.textLayers.map { if (it.id == updatedLayer.id) updatedLayer else it }
        _projectState.value = current.copy(textLayers = list)
    }

    fun deleteTextLayer(layerId: String) {
        val current = _projectState.value
        pushStateToUndo(current)
        _projectState.value = current.copy(textLayers = current.textLayers.filter { it.id != layerId })
        if (_selectedLayerId.value == layerId) {
            _selectedLayerId.value = null
        }
    }

    // --- LOGO LAYERS ---
    fun addLogoLayer(imageUri: Uri) {
        val current = _projectState.value
        pushStateToUndo(current)

        val newLogo = LogoLayer(
            id = UUID.randomUUID().toString(),
            imageUriString = imageUri.toString(),
            positionPreset = LogoPositionPreset.TOP_RIGHT,
            posXFraction = 0.82f,
            posYFraction = 0.12f,
            scale = 0.6f,
            opacity = 0.95f,
            applyFullVideo = true
        )

        val updated = current.copy(logoLayers = current.logoLayers + newLogo)
        _projectState.value = updated
        _selectedLayerId.value = newLogo.id
    }

    fun updateLogoLayer(updatedLogo: LogoLayer) {
        val current = _projectState.value
        val list = current.logoLayers.map { if (it.id == updatedLogo.id) updatedLogo else it }
        _projectState.value = current.copy(logoLayers = list)
    }

    fun deleteLogoLayer(logoId: String) {
        val current = _projectState.value
        pushStateToUndo(current)
        _projectState.value = current.copy(logoLayers = current.logoLayers.filter { it.id != logoId })
        if (_selectedLayerId.value == logoId) {
            _selectedLayerId.value = null
        }
    }

    // --- FRAME LAYER ---
    fun updateFrameLayer(frameLayer: FrameLayer) {
        val current = _projectState.value
        pushStateToUndo(current)
        _projectState.value = current.copy(frameLayer = frameLayer)
    }

    fun selectLayer(id: String?) {
        _selectedLayerId.value = id
    }

    // --- EXPORT CONFIG & RUN ---
    fun updateExportOptions(options: ExportOptions) {
        _exportOptions.value = options
    }

    fun startExport() {
        val current = _projectState.value
        if (current.originalVideoUri.isBlank()) {
            _userMessage.value = "No video loaded to export!"
            return
        }

        _isExporting.value = true
        _exportProgress.value = ExportProgress.Progress(0f, "Starting Export...")

        viewModelScope.launch {
            exportEngine.exportProject(
                state = current,
                options = _exportOptions.value,
                onProgress = { progress ->
                    _exportProgress.value = progress
                    when (progress) {
                        is ExportProgress.Success -> {
                            _isExporting.value = false
                            // Save to exported videos DB
                            viewModelScope.launch {
                                repository.saveExportedVideo(
                                    ExportedVideoEntity(
                                        id = UUID.randomUUID().toString(),
                                        title = "${current.projectName}.mp4",
                                        filePath = progress.outputFilePath,
                                        durationMs = progress.durationMs,
                                        width = _exportOptions.value.targetWidth,
                                        height = _exportOptions.value.targetHeight,
                                        fps = _exportOptions.value.fps,
                                        fileSize = progress.fileSize,
                                        createdAt = System.currentTimeMillis(),
                                        thumbnailUri = current.thumbnailUri
                                    )
                                )
                            }
                        }
                        is ExportProgress.Error -> {
                            _isExporting.value = false
                            _userMessage.value = progress.errorMessage
                        }
                        is ExportProgress.Cancelled -> {
                            _isExporting.value = false
                            _userMessage.value = "Export cancelled."
                        }
                        else -> {}
                    }
                }
            )
        }
    }

    fun cancelExport() {
        exportEngine.cancelExport()
        _isExporting.value = false
    }

    fun deleteExportedVideo(id: String) {
        viewModelScope.launch {
            repository.deleteExportedVideo(id)
            _userMessage.value = "Video deleted."
        }
    }

    fun renameExportedVideo(id: String, newName: String) {
        viewModelScope.launch {
            repository.renameExportedVideo(id, newName)
            _userMessage.value = "Renamed successfully."
        }
    }

    private fun applyStateToPlayer(state: ProjectEditState) {
        playerManager.setTrimRange(state.trimStartMs, state.trimEndMs, enableLoop = true)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
