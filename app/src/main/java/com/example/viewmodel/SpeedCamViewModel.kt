package com.example.viewmodel

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CameraMode
import com.example.model.EcuSimProfile
import com.example.model.GpsStatus
import com.example.model.RecordedVideoItem
import com.example.model.SpeedCamUiState
import com.example.telemetry.SpeedTelemetryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpeedCamViewModel(application: Application) : AndroidViewModel(application) {

    private val telemetryManager = SpeedTelemetryManager(application.applicationContext)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)

    private val _uiState = MutableStateFlow(
        SpeedCamUiState(
            currentDateTimeText = dateFormat.format(Date())
        )
    )
    val uiState: StateFlow<SpeedCamUiState> = _uiState.asStateFlow()

    private var gpsJob: Job? = null
    private var simTickCounter = 0
    private var lastGpsSampleMillis = 0L
    private var latestRealSpeedKmh: Float? = null

    init {
        startOneSecondTelemetryAndClockLoop()
        loadSavedVideosFromMediaStore()
    }

    private fun startOneSecondTelemetryAndClockLoop() {
        viewModelScope.launch {
            while (isActive) {
                val nowMillis = System.currentTimeMillis()
                val formattedClock = dateFormat.format(Date(nowMillis))

                _uiState.update { state ->
                    val nextElapsed = if (state.isRecording) {
                        state.recordingElapsedSeconds + 1L
                    } else {
                        0L
                    }

                    // Determine current speed sample for this 1-second tick
                    val currentSpeed: Float?
                    val nextGpsStatus: GpsStatus

                    if (state.ecuSimProfile != EcuSimProfile.OFF) {
                        simTickCounter++
                        val maxTarget = state.ecuSimProfile.targetMaxKmh
                        // Realistic acceleration + gear shifts + coasting curve
                        val cyclePhase = (simTickCounter % 40) / 40.0
                        val wave = if (cyclePhase < 0.72) {
                            // Acceleration pull with slight gear shift ripples
                            val pull = (cyclePhase / 0.72)
                            val gearRipple = sin(cyclePhase * 18.0) * 0.035
                            (pull + gearRipple).coerceIn(0.0, 1.0)
                        } else {
                            // Deceleration / engine braking
                            val decel = 1.0 - ((cyclePhase - 0.72) / 0.28)
                            (decel * decel).coerceIn(0.0, 1.0)
                        }
                        val simulated = (wave * maxTarget).toFloat().coerceIn(0f, 250f)
                        currentSpeed = simulated
                        nextGpsStatus = GpsStatus.ECU_SIM
                    } else {
                        val gpsFresh = (nowMillis - lastGpsSampleMillis) <= 4500L && latestRealSpeedKmh != null
                        if (gpsFresh) {
                            currentSpeed = latestRealSpeedKmh
                            nextGpsStatus = GpsStatus.LOCKED
                        } else {
                            currentSpeed = null
                            nextGpsStatus = GpsStatus.WAITING
                        }
                    }

                    val updatedMax = when {
                        currentSpeed == null -> state.maxSpeedKmh
                        state.maxSpeedKmh == null -> currentSpeed
                        currentSpeed > state.maxSpeedKmh -> currentSpeed
                        else -> state.maxSpeedKmh
                    }

                    val updatedHistory = (state.speedHistory120s.drop(1) + currentSpeed).takeLast(120)

                    state.copy(
                        currentDateTimeText = formattedClock,
                        recordingElapsedSeconds = nextElapsed,
                        gpsStatus = nextGpsStatus,
                        currentSpeedKmh = currentSpeed,
                        maxSpeedKmh = updatedMax,
                        speedHistory120s = updatedHistory
                    )
                }

                delay(1000L)
            }
        }
    }

    fun startRealGpsTracking() {
        if (gpsJob?.isActive == true) return
        gpsJob = viewModelScope.launch {
            telemetryManager.observeRealGpsSpeed().collect { sample ->
                lastGpsSampleMillis = sample.timestampMillis
                latestRealSpeedKmh = sample.speedKmh
                if (_uiState.value.ecuSimProfile == EcuSimProfile.OFF) {
                    _uiState.update { state ->
                        val newMax = when {
                            state.maxSpeedKmh == null -> sample.speedKmh
                            sample.speedKmh > state.maxSpeedKmh -> sample.speedKmh
                            else -> state.maxSpeedKmh
                        }
                        state.copy(
                            gpsStatus = GpsStatus.LOCKED,
                            currentSpeedKmh = sample.speedKmh,
                            maxSpeedKmh = newMax
                        )
                    }
                }
            }
        }
    }

    fun toggleVideoStabilization() {
        _uiState.update { state ->
            val next = !state.isVideoStabilizationOn
            state.copy(
                isVideoStabilizationOn = next,
                statusToastMessage = if (next) "เปิดระบบกันสั่นวิดีโอแล้ว" else "ปิดระบบกันสั่นวิดีโอแล้ว"
            )
        }
    }

    fun openCameraSelectorDialog() {
        _uiState.update { it.copy(showCameraSelectorDialog = true) }
    }

    fun dismissCameraSelectorDialog() {
        _uiState.update { it.copy(showCameraSelectorDialog = false) }
    }

    fun selectCameraMode(mode: CameraMode) {
        _uiState.update {
            it.copy(
                cameraMode = mode,
                showCameraSelectorDialog = false,
                statusToastMessage = "สลับไปยัง ${mode.dialogLabel}"
            )
        }
    }

    fun swapDualCameraPip() {
        _uiState.update { it.copy(dualPipSwapped = !it.dualPipSwapped) }
    }

    fun toggleMicrophone() {
        _uiState.update { state ->
            val next = !state.isMicOn
            state.copy(
                isMicOn = next,
                statusToastMessage = if (next) "ไมโครโฟน: เปิด" else "ไมโครโฟน: ปิด"
            )
        }
    }

    fun setRecordingActive(active: Boolean) {
        _uiState.update { state ->
            state.copy(
                isRecording = active,
                recordingElapsedSeconds = if (active) 0L else state.recordingElapsedSeconds
            )
        }
    }

    fun onVideoSaved(uri: Uri?, fileName: String, durationSec: Long) {
        val snapshot = _uiState.value
        val newEntry = RecordedVideoItem(
            id = System.currentTimeMillis(),
            uri = uri,
            displayName = fileName,
            dateFormatted = snapshot.currentDateTimeText,
            durationSeconds = durationSec.coerceAtLeast(1L),
            maxSpeedKmh = snapshot.maxSpeedKmh?.roundToInt(),
            cameraModeLabel = snapshot.cameraMode.dialogLabel,
            badgeTitle = snapshot.badgeText
        )
        _uiState.update { state ->
            state.copy(
                isRecording = false,
                recordingElapsedSeconds = 0L,
                recordedVideos = listOf(newEntry) + state.recordedVideos,
                statusToastMessage = "บันทึกวิดีโอสำเร็จ: $fileName"
            )
        }
    }

    fun onVideoRecordingError(message: String) {
        _uiState.update { state ->
            state.copy(
                isRecording = false,
                recordingElapsedSeconds = 0L,
                statusToastMessage = message
            )
        }
    }

    fun openVideoGallerySheet() {
        loadSavedVideosFromMediaStore()
        _uiState.update { it.copy(showVideoGallerySheet = true) }
    }

    fun dismissVideoGallerySheet() {
        _uiState.update { it.copy(showVideoGallerySheet = false) }
    }

    fun openBadgeTuneDialog() {
        _uiState.update { it.copy(showBadgeTuneDialog = true) }
    }

    fun dismissBadgeTuneDialog() {
        _uiState.update { it.copy(showBadgeTuneDialog = false) }
    }

    fun updateBadgeText(newBadge: String) {
        val trimmed = newBadge.trim().ifEmpty { "REC A2A LIVE" }
        _uiState.update {
            it.copy(
                badgeText = trimmed,
                showBadgeTuneDialog = false
            )
        }
    }

    fun selectEcuSimProfile(profile: EcuSimProfile) {
        simTickCounter = 0
        _uiState.update { state ->
            if (profile == EcuSimProfile.OFF) {
                state.copy(
                    ecuSimProfile = EcuSimProfile.OFF,
                    gpsStatus = GpsStatus.WAITING,
                    currentSpeedKmh = null,
                    maxSpeedKmh = null,
                    speedHistory120s = List(120) { null },
                    statusToastMessage = "สลับเป็นโหมด GPS จริง"
                )
            } else {
                state.copy(
                    ecuSimProfile = profile,
                    gpsStatus = GpsStatus.ECU_SIM,
                    statusToastMessage = "เปิด${profile.title}"
                )
            }
        }
    }

    fun resetSpeedHistoryAndMax() {
        simTickCounter = 0
        _uiState.update { state ->
            state.copy(
                maxSpeedKmh = null,
                speedHistory120s = List(120) { null },
                statusToastMessage = "รีเซ็ตค่า Speed Max และกราฟย้อนหลัง 120 วินาทีแล้ว"
            )
        }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(statusToastMessage = null) }
    }

    private fun loadSavedVideosFromMediaStore() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.DURATION
                )
                val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("A2A_LIVE_%")
                val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

                val loaded = mutableListOf<RecordedVideoItem>()
                resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "A2A_LIVE.mp4"
                        val dateSec = cursor.getLong(dateCol)
                        val durMs = cursor.getLong(durCol)
                        val contentUri = ContentUris.withAppendedId(collection, id)
                        loaded.add(
                            RecordedVideoItem(
                                id = id,
                                uri = contentUri,
                                displayName = name,
                                dateFormatted = dateFormat.format(Date(dateSec * 1000L)),
                                durationSeconds = (durMs / 1000L).coerceAtLeast(1L),
                                maxSpeedKmh = null,
                                cameraModeLabel = "บันทึกในเครื่อง",
                                badgeTitle = _uiState.value.badgeText
                            )
                        )
                    }
                }
                if (loaded.isNotEmpty()) {
                    _uiState.update { state ->
                        val existingUris = state.recordedVideos.mapNotNull { it.uri }.toSet()
                        val merged = state.recordedVideos + loaded.filter { it.uri !in existingUris }
                        state.copy(recordedVideos = merged)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
