package com.novachat.ui.financial.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.domain.model.DailySpending
import kotlinx.coroutines.delay
import java.text.DateFormatSymbols
import java.util.Calendar

@Composable
fun SpendingChart(
    dailySpending: List<DailySpending>,
    month: Int,
    year: Int,
    modifier: Modifier = Modifier
) {
    val monthName = DateFormatSymbols().months[month - 1].replaceFirstChar { it.uppercase() }
    val todayDay = Calendar.getInstance().let {
        if (it.get(Calendar.MONTH) + 1 == month && it.get(Calendar.YEAR) == year)
            it.get(Calendar.DAY_OF_MONTH)
        else -1
    }
    val barColor = Color(0xFF26A69A)
    val todayColor = Color(0xFF00BFA5)
    val selectedColor = Color(0xFFFFB300)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = textColor.toArgb()
    val tooltipBgColor = MaterialTheme.colorScheme.inverseSurface
    val tooltipTextColor = MaterialTheme.colorScheme.inverseOnSurface.toArgb()
    val currency = "₪"

    var selectedDay by remember { mutableIntStateOf(-1) }
    var barWidthPx by remember { mutableFloatStateOf(0f) }
    var daysInMonthState by remember { mutableIntStateOf(30) }

    // Auto-dismiss tooltip after 2 seconds of no interaction
    LaunchedEffect(selectedDay) {
        if (selectedDay > 0) {
            delay(2000)
            selectedDay = -1
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (dailySpending.isEmpty()) {
                Text(
                    text = "No spending data yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
                return@Card
            }

            val maxAmount = dailySpending.maxOf { it.total }.coerceAtLeast(1.0)
            val daysInMonth = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)
            daysInMonthState = daysInMonth

            val spendingMap = dailySpending.associate { it.dayOfMonth to it.total }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(daysInMonth) {
                        detectTapGestures { offset ->
                            val bw = size.width.toFloat() / (daysInMonth + 1)
                            val day = ((offset.x / bw) + 0.5f).toInt().coerceIn(1, daysInMonth)
                            selectedDay = if (selectedDay == day) -1 else day
                        }
                    }
                    .pointerInput(daysInMonth) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val bw = size.width.toFloat() / (daysInMonth + 1)
                                selectedDay = ((offset.x / bw) + 0.5f).toInt().coerceIn(1, daysInMonth)
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val bw = size.width.toFloat() / (daysInMonth + 1)
                                selectedDay = ((change.position.x / bw) + 0.5f).toInt().coerceIn(1, daysInMonth)
                            },
                            onDragEnd = { /* keep tooltip visible; LaunchedEffect dismisses it */ }
                        )
                    }
            ) {
                val bw = size.width / (daysInMonth + 1)
                barWidthPx = bw
                val chartHeight = size.height - 24f

                // Grid lines
                for (i in 1..3) {
                    val y = chartHeight * (1 - i / 4f)
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }

                // Bars
                for (day in 1..daysInMonth) {
                    val amount = spendingMap[day] ?: 0.0
                    val barHeight = (amount / maxAmount * chartHeight).toFloat()
                    val x = (day - 0.5f) * bw
                    val color = when {
                        day == selectedDay -> selectedColor
                        day == todayDay -> todayColor
                        else -> barColor
                    }

                    if (barHeight > 0f) {
                        drawRect(
                            color = color,
                            topLeft = Offset(x, chartHeight - barHeight),
                            size = Size(bw * 0.7f, barHeight)
                        )
                    }

                    if (day % 5 == 1 || day == daysInMonth) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "$day",
                            x + bw * 0.35f,
                            size.height,
                            android.graphics.Paint().apply {
                                this.color = labelColor
                                textSize = 24f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }

                // Selected day indicator line + tooltip
                if (selectedDay > 0) {
                    val selX = (selectedDay - 0.5f) * bw + bw * 0.35f
                    drawLine(
                        color = selectedColor.copy(alpha = 0.5f),
                        start = Offset(selX, 0f),
                        end = Offset(selX, chartHeight),
                        strokeWidth = 2f
                    )

                    val amount = spendingMap[selectedDay] ?: 0.0
                    val tooltipText = "Day $selectedDay: $currency${"%.2f".format(amount)}"
                    val paint = android.graphics.Paint().apply {
                        this.color = tooltipTextColor
                        textSize = 30f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val textWidth = paint.measureText(tooltipText)
                    val tooltipW = textWidth + 24f
                    val tooltipH = 52f
                    val tooltipX = (selX - tooltipW / 2f).coerceIn(0f, size.width - tooltipW)
                    val tooltipY = 4f

                    drawRoundRect(
                        color = tooltipBgColor,
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipW, tooltipH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        tooltipText,
                        tooltipX + tooltipW / 2f,
                        tooltipY + tooltipH - 14f,
                        paint
                    )
                }
            }
        }
    }
}
