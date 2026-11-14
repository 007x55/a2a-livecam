package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.camera.CameraPreviewContainer
import com.example.ui.camera.VideoRecordingController
import com.example.ui.components.BadgeAndEcuTuningDialog
import com.example.ui.components.CircularSpeedometerGauge
import com.example.ui.components.RecLiveBadgeOverlay
import com.example.ui.components.RecordedVideosBottomSheet
import com.example.ui.components.SelectCameraDialog
import com.example.ui.components.SpeedHistoryGraphCard
import com.example.ui.components.TimestampPillOverlay
import com.example.ui.theme.BrightTeal
import com.example.ui.theme.ControlButtonBg
import com.example.ui.theme.ControlButtonBorder
import com.example.ui.theme.DarkChromeBg
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OverlayPillBg
import com.example.ui.theme.RecRed
import com.example.ui.theme.RecRedBright
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGray
import com.example.viewmodel.SpeedCamViewModel
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun SpeedCamScreen(
    viewModel: SpeedCamViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recordingController = remember { VideoRecordingController() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val camGranted = result[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        val locGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        hasCameraPermission = camGranted
        hasLocationPermission = locGranted
        if (locGranted) {
            viewModel.startRealGpsTracking()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            viewModel.startRealGpsTracking()
        }
        val needed = mutableListOf<String>()
        if (!hasCameraPermission) needed.add(Manifest.permission.CAMERA)
        if (!hasLocationPermission) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (needed.isNotEmpty()) {
            try {
                permissionsLauncher.launch(needed.toTypedArray())
            } catch (_: Exception) {
            }
        }
    }

    // Auto-clear brief toast banner
    LaunchedEffect(uiState.statusToastMessage) {
        if (uiState.statusToastMessage != null) {
            delay(2200L)
            viewModel.clearToastMessage()
        }
    }

    BackHandler(
        enabled = uiState.showCameraSelectorDialog ||
            uiState.showVideoGallerySheet ||
            uiState.showBadgeTuneDialog
    ) {
        when {
            uiState.showCameraSelectorDialog -> viewModel.dismissCameraSelectorDialog()
            uiState.showBadgeTuneDialog -> viewModel.dismissBadgeTuneDialog()
            uiState.showVideoGallerySheet -> viewModel.dismissVideoGallerySheet()
        }
    }

    Scaffold(
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                // 1. TOP CHROME HEADER: Status Line + Video Stabilization Toggle Pill
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkChromeBg)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val statusActionText = if (uiState.isRecording) "กำลังบันทึก" else "พร้อมบันทึก"
                    val statusLine =
                        "$statusActionText • ${uiState.cameraMode.statusModeLabel} • กราฟย้อนหลัง 120 วินาที"

                    Text(
                        text = statusLine,
                        color = BrightTeal,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .testTag("top_status_text")
                    )

                    // Video Stabilization Pill Button ("กันสั่นวิดีโอ: ปิด" / "กันสั่นวิดีโอ: เปิด")
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ControlButtonBg)
                            .border(
                                width = 1.dp,
                                color = if (uiState.isVideoStabilizationOn) {
                                    BrightTeal.copy(alpha = 0.5f)
                                } else {
                                    ControlButtonBorder
                                },
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { viewModel.toggleVideoStabilization() }
                            .testTag("video_stabilization_button")
                    ) {
                        Text(
                            text = if (uiState.isVideoStabilizationOn) {
                                "กันสั่นวิดีโอ: เปิด"
                            } else {
                                "กันสั่นวิดีโอ: ปิด"
                            },
                            color = if (uiState.isVideoStabilizationOn) {
                                BrightTeal
                            } else {
                                TextPrimaryWhite
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 2. CENTER CAMERA PREVIEW FRAME + TELEMETRY OVERLAYS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(ObsidianBlack)
                        .padding(horizontal = 18.dp)
                ) {
                    CameraPreviewContainer(
                        hasCameraPermission = hasCameraPermission,
                        cameraMode = uiState.cameraMode,
                        isStabilizationOn = uiState.isVideoStabilizationOn,
                        dualPipSwapped = uiState.dualPipSwapped,
                        onSwapDualPip = { viewModel.swapDualCameraPip() },
                        recordingController = recordingController,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top-Left Badge: ● REC NK LIVE
                    RecLiveBadgeOverlay(
                        badgeText = uiState.badgeText,
                        isRecording = uiState.isRecording,
                        onClick = { viewModel.openBadgeTuneDialog() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 12.dp, start = 10.dp)
                    )

                    // Subtle status toast inside top-center of preview when switching modes
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.statusToastMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 52.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(OverlayPillBg)
                                .border(1.dp, BrightTeal.copy(alpha = 0.4f), RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = uiState.statusToastMessage.orEmpty(),
                                color = BrightTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Bottom Overlay Stack: Circular Speedometer + Timestamp Pill + SPEED HISTORY Graph Card
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        CircularSpeedometerGauge(
                            gpsStatus = uiState.gpsStatus,
                            currentSpeedKmh = uiState.currentSpeedKmh,
                            onClick = { viewModel.openBadgeTuneDialog() }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TimestampPillOverlay(
                            dateTimeText = uiState.currentDateTimeText
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        SpeedHistoryGraphCard(
                            speedHistory120s = uiState.speedHistory120s,
                            maxSpeedKmh = uiState.maxSpeedKmh,
                            onCardClick = { viewModel.openBadgeTuneDialog() }
                        )
                    }
                }

                // 3. BOTTOM RECORDING TIMER & 4-BUTTON CONTROL DOCK
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkChromeBg)
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatElapsedSeconds(uiState.recordingElapsedSeconds),
                        color = if (uiState.isRecording) RecRedBright else Color(0xFFD8E0E8),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .testTag("recording_timer_text")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Button 1: กล้องหลัง / กล้องหน้า / หน้า+หลัง
                        DockControlButton(
                            onClick = { viewModel.openCameraSelectorDialog() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("camera_select_button")
                        ) {
                            Text(
                                text = uiState.cameraMode.buttonLabel,
                                color = TextPrimaryWhite,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        // Button 2: ● REC / ■ STOP
                        DockControlButton(
                            onClick = {
                                recordingController.startOrStopRecording(
                                    context = context,
                                    isCurrentlyRecording = uiState.isRecording,
                                    isMicEnabled = uiState.isMicOn,
                                    onRecordingStateChanged = { active ->
                                        viewModel.setRecordingActive(active)
                                    },
                                    onVideoSaved = { uri, name, dur ->
                                        viewModel.onVideoSaved(uri, name, dur)
                                    },
                                    onError = { msg ->
                                        viewModel.onVideoRecordingError(msg)
                                    }
                                )
                            },
                            isHighlighted = uiState.isRecording,
                            highlightColor = RecRedBright,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rec_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(if (uiState.isRecording) RoundedCornerShape(2.dp) else CircleShape)
                                        .background(if (uiState.isRecording) RecRedBright else RecRed)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = if (uiState.isRecording) "STOP" else "REC",
                                    color = if (uiState.isRecording) RecRedBright else RecRed,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Button 3: ไมค์ เปิด / ไมค์ ปิด
                        DockControlButton(
                            onClick = { viewModel.toggleMicrophone() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mic_toggle_button")
                        ) {
                            Text(
                                text = if (uiState.isMicOn) "ไมค์ เปิด" else "ไมค์ ปิด",
                                color = if (uiState.isMicOn) TextPrimaryWhite else TextSecondaryGray,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        // Button 4: วิดีโอ
                        DockControlButton(
                            onClick = { viewModel.openVideoGallerySheet() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("videos_button")
                        ) {
                            Text(
                                text = "วิดีโอ",
                                color = TextPrimaryWhite,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Dialog 1: เลือกกล้อง (Screenshot 1 modal)
            if (uiState.showCameraSelectorDialog) {
                SelectCameraDialog(
                    currentMode = uiState.cameraMode,
                    onSelectMode = { mode -> viewModel.selectCameraMode(mode) },
                    onDismiss = { viewModel.dismissCameraSelectorDialog() }
                )
            }

            // Dialog 2: ตั้งค่าป้ายชื่อ (REC NK LIVE / AEKBOY2010) & จำลองจูนกล่อง ECU
            if (uiState.showBadgeTuneDialog) {
                BadgeAndEcuTuningDialog(
                    currentBadgeText = uiState.badgeText,
                    currentSimProfile = uiState.ecuSimProfile,
                    onSaveBadgeText = { text -> viewModel.updateBadgeText(text) },
                    onSelectSimProfile = { profile -> viewModel.selectEcuSimProfile(profile) },
                    onResetMaxSpeed = { viewModel.resetSpeedHistoryAndMax() },
                    onDismiss = { viewModel.dismissBadgeTuneDialog() }
                )
            }

            // Bottom Sheet: วิดีโอที่บันทึก
            if (uiState.showVideoGallerySheet) {
                RecordedVideosBottomSheet(
                    videos = uiState.recordedVideos,
                    currentSimProfile = uiState.ecuSimProfile,
                    onOpenTuningSettings = { viewModel.openBadgeTuneDialog() },
                    onResetMaxSpeed = { viewModel.resetSpeedHistoryAndMax() },
                    onDismiss = { viewModel.dismissVideoGallerySheet() }
                )
            }
        }
    }
}

@Composable
private fun DockControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    highlightColor: Color = BrightTeal,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ControlButtonBg)
            .border(
                width = 1.dp,
                color = if (isHighlighted) highlightColor.copy(alpha = 0.65f) else ControlButtonBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp)
    ) {
        content()
    }
}

private fun formatElapsedSeconds(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
}
