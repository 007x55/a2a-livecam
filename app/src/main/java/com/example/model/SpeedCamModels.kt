package com.example.model

import android.net.Uri

enum class CameraMode(
    val dialogLabel: String,
    val buttonLabel: String,
    val statusModeLabel: String
) {
    BACK(
        dialogLabel = "กล้องหลัง",
        buttonLabel = "กล้องหลัง",
        statusModeLabel = "กล้องเดี่ยว"
    ),
    FRONT(
        dialogLabel = "กล้องหน้า",
        buttonLabel = "กล้องหน้า",
        statusModeLabel = "กล้องเดี่ยว"
    ),
    DUAL(
        dialogLabel = "หน้า + หลัง พร้อมกัน",
        buttonLabel = "หน้า+หลัง",
        statusModeLabel = "หน้า+หลัง"
    )
}

enum class GpsStatus {
    WAITING,
    LOCKED,
    ECU_SIM
}

enum class EcuSimProfile(val title: String, val targetMaxKmh: Float) {
    OFF("GPS จริง (รอสัญญาณดาวเทียม)", 0f),
    STREET_TUNE("จำลองจูนกล่อง Street (0 - 135 km/h)", 135f),
    DRAG_TUNE("จำลองจูนกล่อง Drag (0 - 215 km/h)", 215f)
}

data class RecordedVideoItem(
    val id: Long,
    val uri: Uri?,
    val displayName: String,
    val dateFormatted: String,
    val durationSeconds: Long,
    val maxSpeedKmh: Int?,
    val cameraModeLabel: String,
    val badgeTitle: String
)

data class SpeedCamUiState(
    val cameraMode: CameraMode = CameraMode.BACK,
    val isVideoStabilizationOn: Boolean = false,
    val isMicOn: Boolean = true,
    val isRecording: Boolean = false,
    val recordingElapsedSeconds: Long = 0L,
    val showCameraSelectorDialog: Boolean = false,
    val showVideoGallerySheet: Boolean = false,
    val showBadgeTuneDialog: Boolean = false,
    val gpsStatus: GpsStatus = GpsStatus.WAITING,
    val currentSpeedKmh: Float? = null,
    val maxSpeedKmh: Float? = null,
    // 120 seconds of speed history (1 sample per second, index 0 = -120s, index 119 = NOW)
    val speedHistory120s: List<Float?> = List(120) { null },
    val currentDateTimeText: String = "",
    val badgeText: String = "REC A2A LIVE",
    val ecuSimProfile: EcuSimProfile = EcuSimProfile.OFF,
    val recordedVideos: List<RecordedVideoItem> = emptyList(),
    val statusToastMessage: String? = null,
    val dualPipSwapped: Boolean = false
)
