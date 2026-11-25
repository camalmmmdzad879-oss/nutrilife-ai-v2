package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.i18n.LocalStrings
import kotlin.math.roundToInt

@Composable
fun CalorieProgressRing(
    consumed: Int,
    target: Int,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val safeTarget = target.coerceAtLeast(1)
    val rawProgress = (consumed.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1.25f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress.coerceAtMost(1f),
        animationSpec = tween(durationMillis = 500),
        label = "calorie_ring"
    )
    val isOver = consumed > target
    val primaryColor = if (isOver) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier.size(164.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(164.dp)) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryColor,
                startAngle = 140f,
                sweepAngle = 260f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val diff = kotlin.math.abs(target - consumed)
            Text(
                text = diff.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isOver) strings.caloriesOver else strings.caloriesRemaining,
                style = MaterialTheme.typography.labelMedium,
                color = if (isOver) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$consumed / $target ${strings.kcal}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    currentG: Double,
    targetG: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val progress = (currentG.toFloat() / targetG.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${currentG.roundToInt()}/$targetG ${strings.gramsUnit}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
fun LanguageSelectorRow(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppLanguage.entries.forEach { lang ->
            val isSelected = lang == selected
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .testTag("lang_chip_${lang.code}")
                    .clickable { onSelect(lang) }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (lang) {
                            AppLanguage.AZ -> "AZ"
                            AppLanguage.EN -> "EN"
                            AppLanguage.RU -> "RU"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun InfoNoticeBanner(
    icon: ImageVector,
    title: String? = null,
    body: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title ?: body,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    values: List<Pair<String, Float>>,
    targetLine: Float? = null,
    barColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(
        values.maxOfOrNull { it.second } ?: 1f,
        targetLine ?: 1f,
        1f
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val targetLineColor = MaterialTheme.colorScheme.tertiary

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (values.isEmpty()) return@Canvas
            val count = values.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.54f).coerceAtMost(36.dp.toPx())

            if (targetLine != null && targetLine > 0f) {
                val yTarget = size.height - (targetLine / maxVal).coerceIn(0f, 1f) * size.height
                drawLine(
                    color = targetLineColor.copy(alpha = 0.7f),
                    start = Offset(0f, yTarget),
                    end = Offset(size.width, yTarget),
                    strokeWidth = 2.dp.toPx()
                )
            }

            values.forEachIndexed { idx, (_, value) ->
                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                val h = (value / maxVal).coerceIn(0f, 1f) * (size.height - 6.dp.toPx())
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
                if (h > 0f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            values.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SimpleLineChart(
    points: List<Pair<String, Float>>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val minVal = (points.minOf { it.second } - 2f).coerceAtLeast(0f)
    val maxVal = (points.maxOf { it.second } + 2f).coerceAtLeast(minVal + 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            val path = Path()
            val span = (maxVal - minVal).coerceAtLeast(1f)
            points.forEachIndexed { index, (_, v) ->
                val x = if (points.size == 1) size.width / 2f else (index.toFloat() / (points.size - 1)) * size.width
                val y = size.height - ((v - minVal) / span) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            points.forEachIndexed { index, (_, v) ->
                val x = if (points.size == 1) size.width / 2f else (index.toFloat() / (points.size - 1)) * size.width
                val y = size.height - ((v - minVal) / span) * size.height
                drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = Offset(x, y))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(points.first().first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (points.size > 1) {
                Text(points.last().first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
