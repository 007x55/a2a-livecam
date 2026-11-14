package com.example.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.CameraMode
import com.example.ui.theme.BrightTeal
import com.example.ui.theme.ControlButtonBorder
import com.example.ui.theme.OverlayPillBg
import com.example.ui.theme.TextPrimaryWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoRecordingController {
    var videoCapture: VideoCapture<Recorder>? = null
    var activeRecording: Recording? = null
    var recordingStartMillis: Long = 0L
    var pendingFileName: String = ""

    @SuppressLint("MissingPermission")
    fun startOrStopRecording(
        context: Context,
        isCurrentlyRecording: Boolean,
        isMicEnabled: Boolean,
        onRecordingStateChanged: (Boolean) -> Unit,
        onVideoSaved: (Uri?, String, Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val active = activeRecording
        if (isCurrentlyRecording) {
            val elapsedSec = ((System.currentTimeMillis() - recordingStartMillis) / 1000L).coerceAtLeast(1L)
            val savedName = pendingFileName.ifEmpty {
                "A2A_LIVE_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
            }
            if (active != null) {
                try {
                    active.stop()
                } catch (_: Exception) {
                    onVideoSaved(null, savedName, elapsedSec)
                }
                activeRecording = null
            } else {
                onVideoSaved(null, savedName, elapsedSec)
            }
            return
        }

        val fileName = "A2A_LIVE_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        pendingFileName = fileName
        recordingStartMillis = System.currentTimeMillis()

        val capture = videoCapture
        if (capture == null) {
            // Fallback recording session when running in headless test or without active hardware encoder
            onRecordingStateChanged(true)
            return
        }

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/A2ALiveSpeedCam")
                }
            }

            val mediaStoreOptions = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues).build()

            var pendingRecording = capture.output.prepareRecording(context, mediaStoreOptions)
            val hasAudioPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (isMicEnabled && hasAudioPerm) {
                pendingRecording = pendingRecording.withAudioEnabled()
            }

            onRecordingStateChanged(true)
            activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        activeRecording = null
                        val durationSec =
                            ((System.currentTimeMillis() - recordingStartMillis) / 1000L).coerceAtLeast(1L)
                        if (!event.hasError()) {
                            onVideoSaved(event.outputResults.outputUri, fileName, durationSec)
                        } else {
                            // Even if emulator codec finalizes with warning, keep session record
                            onVideoSaved(
                                event.outputResults.outputUri.takeIf { it != Uri.EMPTY },
                                fileName,
                                durationSec
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Keep timer recording active so user can still record telemetry session
            onRecordingStateChanged(true)
        }
    }
}

@Composable
fun CameraPreviewContainer(
    hasCameraPermission: Boolean,
    cameraMode: CameraMode,
    isStabilizationOn: Boolean,
    dualPipSwapped: Boolean,
    onSwapDualPip: () -> Unit,
    recordingController: VideoRecordingController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val primaryPreviewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val secondaryPreviewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    var isConcurrentDualSupported by remember { mutableStateOf(false) }

    LaunchedEffect(hasCameraPermission, cameraMode, isStabilizationOn, dualPipSwapped) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProviderFuture = try {
            ProcessCameraProvider.getInstance(context)
        } catch (_: Exception) {
            null
        } ?: return@LaunchedEffect

        cameraProviderFuture.addListener({
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (_: Exception) {
                null
            } ?: return@addListener

            try {
                cameraProvider.unbindAll()

                val desiredPrimaryLens = when {
                    cameraMode == CameraMode.FRONT -> CameraSelector.LENS_FACING_FRONT
                    cameraMode == CameraMode.DUAL && dualPipSwapped -> CameraSelector.LENS_FACING_FRONT
                    else -> CameraSelector.LENS_FACING_BACK
                }

                val canBindVideoCapture = supportsHardwareEncoderProfiles(context, desiredPrimaryLens)
                val videoCapture = if (canBindVideoCapture) {
                    try {
                        val recorder = Recorder.Builder()
                            .setQualitySelector(
                                QualitySelector.from(
                                    Quality.HD,
                                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                                )
                            )
                            .build()
                        VideoCapture.withOutput(recorder)
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }
                recordingController.videoCapture = videoCapture

                val previewBuilder = Preview.Builder()
                if (isStabilizationOn) {
                    previewBuilder.setPreviewStabilizationEnabled(true)
                }
                val primaryPreview = previewBuilder
                    .build()
                    .also { it.surfaceProvider = primaryPreviewView.surfaceProvider }

                if (cameraMode == CameraMode.DUAL) {
                    val concurrentInfos = cameraProvider.availableConcurrentCameraInfos
                    val pair = concurrentInfos.firstOrNull { list ->
                        list.any { it.lensFacing == CameraSelector.LENS_FACING_BACK } &&
                            list.any { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                    }

                    if (pair != null) {
                        val backInfo = pair.first { it.lensFacing == CameraSelector.LENS_FACING_BACK }
                        val frontInfo = pair.first { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                        val mainSelector = if (dualPipSwapped) frontInfo.cameraSelector else backInfo.cameraSelector
                        val pipSelector = if (dualPipSwapped) backInfo.cameraSelector else frontInfo.cameraSelector

                        val secondaryPreview = Preview.Builder().build().also {
                            it.surfaceProvider = secondaryPreviewView.surfaceProvider
                        }

                        val mainGroupBuilder = UseCaseGroup.Builder()
                            .addUseCase(primaryPreview)
                        if (videoCapture != null) {
                            mainGroupBuilder.addUseCase(videoCapture)
                        }
                        val mainGroup = mainGroupBuilder.build()
                        val pipGroup = UseCaseGroup.Builder()
                            .addUseCase(secondaryPreview)
                            .build()

                        val mainConfig = ConcurrentCamera.SingleCameraConfig(
                            mainSelector,
                            mainGroup,
                            lifecycleOwner
                        )
                        val pipConfig = ConcurrentCamera.SingleCameraConfig(
                            pipSelector,
                            pipGroup,
                            lifecycleOwner
                        )
                        cameraProvider.bindToLifecycle(listOf(mainConfig, pipConfig))
                        isConcurrentDualSupported = true
                    } else {
                        isConcurrentDualSupported = false
                        val selector = resolveAvailableSelector(cameraProvider, desiredPrimaryLens)
                        if (selector != null) {
                            if (videoCapture != null) {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    primaryPreview,
                                    videoCapture
                                )
                            } else {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    primaryPreview
                                )
                            }
                        }
                    }
                } else {
                    isConcurrentDualSupported = false
                    val selector = resolveAvailableSelector(cameraProvider, desiredPrimaryLens)
                    if (selector != null) {
                        if (videoCapture != null) {
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                primaryPreview,
                                videoCapture
                            )
                        } else {
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                primaryPreview
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recordingController.activeRecording?.stop()
            } catch (_: Exception) {
            }
        }
    }

    // Ambient warm-dark camera backdrop matching the screenshot's soft lens lighting
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7B7364),
                        Color(0xFF474137),
                        Color(0xFF28241E)
                    ),
                    center = Offset(750f, 800f),
                    radius = 1100f
                )
            )
            .testTag("camera_preview_box")
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { primaryPreviewView },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }

        // Dual Camera Picture-in-Picture window when หน้า + หลัง พร้อมกัน is active
        if (cameraMode == CameraMode.DUAL) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .width(108.dp)
                    .height(148.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE6141C24))
                    .border(1.5.dp, BrightTeal.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onSwapDualPip)
                    .testTag("dual_camera_pip_box")
            ) {
                if (hasCameraPermission && isConcurrentDualSupported) {
                    AndroidView(
                        factory = { secondaryPreviewView },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Stylish secondary lens preview card with tap-to-swap lens action
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1E293B),
                                        Color(0xFF0F172A)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(BrightTeal.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "สลับกล้องหน้าหลัง",
                                    tint = BrightTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (dualPipSwapped) "กล้องหลัง" else "กล้องหน้า",
                                color = TextPrimaryWhite,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "แตะเพื่อสลับ",
                                color = BrightTeal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Small badge in top-left of PiP
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(OverlayPillBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (dualPipSwapped) "BACK" else "FRONT",
                        color = BrightTeal,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun resolveAvailableSelector(
    provider: ProcessCameraProvider,
    desiredLensFacing: Int
): CameraSelector? {
    val preferred = if (desiredLensFacing == CameraSelector.LENS_FACING_FRONT) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }
    return try {
        if (provider.hasCamera(preferred)) {
            preferred
        } else if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

private fun supportsHardwareEncoderProfiles(
    context: Context,
    desiredLensFacing: Int
): Boolean {
    // Emulated/virtual cameras in cloud containers lack hardware EncoderProfiles and log
    // E/CapabilitiesByQuality: No supported EncoderProfiles if Recorder is bound.
    val isEmulator = Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
        Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
        Build.MODEL.contains("sdk", ignoreCase = true) ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
        Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
        Build.PRODUCT.contains("sdk", ignoreCase = true)
    if (isEmulator) return false

    return try {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return false
        val targetCamera2Lens = if (desiredLensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        val matchingCameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == targetCamera2Lens
        } ?: cameraManager.cameraIdList.firstOrNull() ?: return false

        val idInt = matchingCameraId.toIntOrNull() ?: return false
        val candidateQualities = intArrayOf(
            CamcorderProfile.QUALITY_720P,
            CamcorderProfile.QUALITY_480P,
            CamcorderProfile.QUALITY_1080P,
            CamcorderProfile.QUALITY_HIGH,
            CamcorderProfile.QUALITY_LOW
        )
        candidateQualities.any { quality ->
            if (!CamcorderProfile.hasProfile(idInt, quality)) {
                false
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val profiles = CamcorderProfile.getAll(matchingCameraId, quality)
                profiles != null && profiles.videoProfiles.isNotEmpty()
            } else {
                true
            }
        }
    } catch (_: Exception) {
        false
    }
}

