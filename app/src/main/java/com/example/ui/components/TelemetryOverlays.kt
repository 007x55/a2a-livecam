package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsStatus
import com.example.ui.theme.BrightTeal
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GridLineGray
import com.example.ui.theme.OverlayCardBg
import com.example.ui.theme.OverlayPillBg
import com.example.ui.theme.RecRedBright
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGray
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RecLiveBadgeOverlay(
    badgeText: String,
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 0.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_dot_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(OverlayPillBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("rec_live_badge")
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording) {
                        RecRedBright.copy(alpha = pulseAlpha)
                    } else {
                        Color(0xFF7B8794)
                    }
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = badgeText,
            color = TextPrimaryWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun CircularSpeedometerGauge(
    gpsStatus: GpsStatus,
    currentSpeedKmh: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startAngle = 135f
    val sweepTotal = 270f
    val maxGaugeSpeed = 250f
    val normalizedSpeed = ((currentSpeedKmh ?: 0f) / maxGaugeSpeed).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(106.dp)
            .clip(CircleShape)
            .background(Color(0xD910161D))
            .clickable(onClick = onClick)
            .testTag("speedometer_gauge")
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
            val strokeWidth = 5.5.dp.toPx()
            val arcDiameter = size.minDimension - strokeWidth
            val topLeftOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(arcDiameter, arcDiameter)

            // Outer subtle dark border ring
            drawArc(
                color = Color(0xFF26323F),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active speed cyan arc
            if (normalizedSpeed > 0.005f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(BrightTeal, ElectricCyan, BrightTeal)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * normalizedSpeed,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Indicator dot along the gauge arc (at start angle when waiting/0, or at current speed tip)
            val dotAngleDeg = startAngle + (sweepTotal * normalizedSpeed)
            val dotAngleRad = Math.toRadians(dotAngleDeg.toDouble())
            val radius = arcDiameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val dotCenter = Offset(
                x = center.x + (radius * cos(dotAngleRad)).toFloat(),
                y = center.y + (radius * sin(dotAngleRad)).toFloat()
            )
            drawCircle(
                color = BrightTeal,
                radius = 3.6.dp.toPx(),
                center = dotCenter
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = when (gpsStatus) {
                    GpsStatus.WAITING -> "GPS WAIT"
                    GpsStatus.LOCKED -> "GPS LIVE"
                    GpsStatus.ECU_SIM -> "ECU SIM"
                },
                color = BrightTeal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (currentSpeedKmh == null) "--" else currentSpeedKmh.roundToInt().toString(),
                color = TextPrimaryWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "km/h",
                color = TextSecondaryGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TimestampPillOverlay(
    dateTimeText: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(210.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(OverlayPillBg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("timestamp_pill")
    ) {
        Text(
            text = dateTimeText,
            color = Color(0xFFB0BAC5),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SpeedHistoryGraphCard(
    speedHistory120s: List<Float?>,
    maxSpeedKmh: Float?,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxSpeedLabel = if (maxSpeedKmh == null) {
        "Speed Max: -- km/h"
    } else {
        "Speed Max: ${maxSpeedKmh.roundToInt()} km/h"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(OverlayCardBg)
            .clickable(onClick = onCardClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("speed_history_card")
    ) {
        // Header Row: SPEED HISTORY and Speed Max: -- km/h
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SPEED HISTORY",
                color = Color(0xFFCFD8E3),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
            Text(
                text = maxSpeedLabel,
                color = BrightTeal,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Unit label km/h above Y-axis
        Text(
            text = "km/h",
            color = BrightTeal.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp)
        )

        // Graph Body: Y-axis labels on left + Canvas grid and line chart on right
        val yLevels = listOf(250, 200, 150, 100, 50, 0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(26.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                yLevels.forEach { level ->
                    Text(
                        text = level.toString(),
                        color = TextSecondaryGray,
                        fontSize = 9.5.sp,
                        lineHeight = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
            ) {
                val chartW = size.width
                val chartH = size.height
                val rowCount = yLevels.size - 1

                // Draw 6 horizontal grid lines (250, 200, 150, 100, 50, 0)
                for (i in 0..rowCount) {
                    val y = (chartH / rowCount) * i
                    drawLine(
                        color = GridLineGray,
                        start = Offset(0f, y),
                        end = Offset(chartW, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Plot the 120s speed line
                val totalPoints = 120
                val stepX = if (totalPoints > 1) chartW / (totalPoints - 1) else chartW

                val validSegments = mutableListOf<List<Offset>>()
                var currentSegment = mutableListOf<Offset>()

                speedHistory120s.forEachIndexed { index, speed ->
                    if (speed != null) {
                        val x = index * stepX
                        val clamped = speed.coerceIn(0f, 250f)
                        val y = chartH - (clamped / 250f) * chartH
                        currentSegment.add(Offset(x, y))
                    } else if (currentSegment.isNotEmpty()) {
                        validSegments.add(currentSegment)
                        currentSegment = mutableListOf()
                    }
                }
                if (currentSegment.isNotEmpty()) {
                    validSegments.add(currentSegment)
                }

                if (validSegments.isEmpty()) {
                    // Draw the signature small cyan tick at NOW (right edge near 0 km/h) when waiting
                    drawLine(
                        color = BrightTeal,
                        start = Offset(chartW - 14.dp.toPx(), chartH - 3.dp.toPx()),
                        end = Offset(chartW - 2.dp.toPx(), chartH - 1.dp.toPx()),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    validSegments.forEach { segment ->
                        if (segment.size == 1) {
                            val pt = segment.first()
                            drawLine(
                                color = BrightTeal,
                                start = Offset((pt.x - 8.dp.toPx()).coerceAtLeast(0f), pt.y),
                                end = pt,
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        } else {
                            val linePath = Path().apply {
                                moveTo(segment.first().x, segment.first().y)
                                for (i in 1 until segment.size) {
                                    lineTo(segment[i].x, segment[i].y)
                                }
                            }
                            val fillPath = Path().apply {
                                addPath(linePath)
                                lineTo(segment.last().x, chartH)
                                lineTo(segment.first().x, chartH)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        BrightTeal.copy(alpha = 0.28f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = chartH
                                )
                            )
                            drawPath(
                                path = linePath,
                                color = BrightTeal,
                                style = Stroke(
                                    width = 2.2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // X-axis labels: -120s, -60s, NOW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "-120s",
                color = TextSecondaryGray,
                fontSize = 9.5.sp
            )
            Text(
                text = "-60s",
                color = TextSecondaryGray,
                fontSize = 9.5.sp
            )
            Text(
                text = "NOW",
                color = BrightTeal,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
