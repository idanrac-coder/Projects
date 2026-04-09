package com.novachat.ui.financial.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novachat.domain.model.DailySpending
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
    val gridColor = Color(0x30FFFFFF)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
            return
        }

        val maxAmount = dailySpending.maxOf { it.total }.coerceAtLeast(1.0)
        val daysInMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        val spendingMap = dailySpending.associate { it.dayOfMonth to it.total }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val barWidth = size.width / (daysInMonth + 1)
            val chartHeight = size.height - 20f

            for (i in 1..3) {
                val y = chartHeight * (1 - i / 4f)
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }

            for (day in 1..daysInMonth) {
                val amount = spendingMap[day] ?: 0.0
                val barHeight = (amount / maxAmount * chartHeight).toFloat()
                val x = (day - 0.5f) * barWidth
                val color = if (day == todayDay) todayColor else barColor

                drawRect(
                    color = color,
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(barWidth * 0.7f, barHeight)
                )

                if (day % 5 == 1 || day == daysInMonth) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "$day",
                        x + barWidth * 0.35f,
                        size.height,
                        android.graphics.Paint().apply {
                            this.color = 0x99FFFFFF.toInt()
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}
