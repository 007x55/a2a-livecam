package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CameraMode
import com.example.model.EcuSimProfile
import com.example.model.RecordedVideoItem
import com.example.ui.theme.BrightTeal
import com.example.ui.theme.ControlButtonBg
import com.example.ui.theme.ControlButtonBorder
import com.example.ui.theme.DarkChromeBg
import com.example.ui.theme.DialogSurfaceBg
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGray

@Composable
fun SelectCameraDialog(
    currentMode: CameraMode,
    onSelectMode: (CameraMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(DialogSurfaceBg)
                .padding(vertical = 22.dp)
                .testTag("select_camera_dialog")
        ) {
            Text(
                text = "เลือกกล้อง",
                color = TextPrimaryWhite,
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            CameraMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMode(mode) }
                        .padding(horizontal = 24.dp, vertical = 15.dp)
                        .testTag("camera_option_${mode.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = mode.dialogLabel,
                        color = TextPrimaryWhite,
                        fontSize = 17.sp,
                        fontWeight = if (mode == currentMode) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (mode == currentMode) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrightTeal)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeAndEcuTuningDialog(
    currentBadgeText: String,
    currentSimProfile: EcuSimProfile,
    onSaveBadgeText: (String) -> Unit,
    onSelectSimProfile: (EcuSimProfile) -> Unit,
    onResetMaxSpeed: () -> Unit,
    onDismiss: () -> Unit
) {
    var badgeInput by remember(currentBadgeText) { mutableStateOf(currentBadgeText) }
    val presets = listOf(
        "REC A2A LIVE",
        "AEKBOY2010 จูนกล่องควบคุม",
        "A2A LIVE DYNO GPS"
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF121A24))
                .border(1.dp, ControlButtonBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .testTag("badge_ecu_tuning_dialog")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = BrightTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ตั้งค่าป้ายชื่อ & โหมดจูนกล่อง",
                        color = TextPrimaryWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ปิด",
                        tint = TextSecondaryGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ข้อความป้ายมุมซ้ายบน (Watermark Badge)",
                color = TextSecondaryGray,
                fontSize = 12.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = badgeInput,
                onValueChange = { badgeInput = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrightTeal,
                    unfocusedBorderColor = ControlButtonBorder,
                    focusedTextColor = TextPrimaryWhite,
                    unfocusedTextColor = TextPrimaryWhite,
                    cursorColor = BrightTeal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("badge_text_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (badgeInput == preset) BrightTeal.copy(alpha = 0.2f)
                                else ControlButtonBg
                            )
                            .border(
                                1.dp,
                                if (badgeInput == preset) BrightTeal else ControlButtonBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { badgeInput = preset }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            color = if (badgeInput == preset) BrightTeal else TextSecondaryGray,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = ControlButtonBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "โหมดสัญญาณความเร็ว (GPS / จำลองจูนกล่อง ECU)",
                color = TextSecondaryGray,
                fontSize = 12.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            EcuSimProfile.entries.forEach { profile ->
                val selected = profile == currentSimProfile
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) BrightTeal.copy(alpha = 0.16f) else ControlButtonBg)
                        .border(
                            1.dp,
                            if (selected) BrightTeal else ControlButtonBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectSimProfile(profile) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("sim_profile_${profile.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = profile.title,
                        color = if (selected) BrightTeal else TextPrimaryWhite,
                        fontSize = 13.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = BrightTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onResetMaxSpeed,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryWhite),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_max_speed_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("รีเซ็ตกราฟ", fontSize = 13.sp)
                }

                Button(
                    onClick = { onSaveBadgeText(badgeInput) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrightTeal,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_badge_button")
                ) {
                    Text("บันทึกค่า", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordedVideosBottomSheet(
    videos: List<RecordedVideoItem>,
    currentSimProfile: EcuSimProfile,
    onOpenTuningSettings: () -> Unit,
    onResetMaxSpeed: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkChromeBg,
        contentColor = TextPrimaryWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("recorded_videos_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = BrightTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "วิดีโอที่บันทึก (${videos.size})",
                            color = TextPrimaryWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "โหมดปัจจุบัน: ${currentSimProfile.title}",
                            color = BrightTeal,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenTuningSettings()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrightTeal),
                        modifier = Modifier.testTag("open_tuning_from_videos_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("จูนกล่อง/จำลอง", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (videos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ControlButtonBg)
                        .border(1.dp, ControlButtonBorder, RoundedCornerShape(14.dp))
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = TextSecondaryGray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ยังไม่มีวิดีโอที่บันทึกในรอบนี้",
                            color = TextPrimaryWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "กดปุ่ม ● REC เพื่อเริ่มบันทึกวิดีโอพร้อมกราฟความเร็วย้อนหลัง 120 วินาที หรือแตะที่ปุ่ม 'จูนกล่อง/จำลอง' เพื่อทดสอบกราฟความเร็ว",
                            color = TextSecondaryGray,
                            fontSize = 12.5.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(videos, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ControlButtonBg)
                                .border(1.dp, ControlButtonBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    item.uri?.let { uri ->
                                        try {
                                            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "video/*")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(playIntent)
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.displayName,
                                    color = TextPrimaryWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.dateFormatted} • ${item.cameraModeLabel} • ${item.durationSeconds}s",
                                    color = TextSecondaryGray,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val maxSpeedStr = item.maxSpeedKmh?.let { "$it km/h" } ?: "-- km/h"
                                Text(
                                    text = "Speed Max: $maxSpeedStr • ${item.badgeTitle}",
                                    color = BrightTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (item.uri != null) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(BrightTeal.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "เล่นวิดีโอ",
                                        tint = BrightTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onResetMaxSpeed,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ล้างค่า Speed Max", fontSize = 12.5.sp)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ControlButtonBg,
                        contentColor = TextPrimaryWhite
                    )
                ) {
                    Text("ปิดหน้าต่าง", fontSize = 12.5.sp)
                }
            }
        }
    }
}
