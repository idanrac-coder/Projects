package com.novachat.ui.financial.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ShimmerBase = Color(0xFF2A2A2A)
private val ShimmerHighlight = Color(0xFF3D3D3D)

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "shimmerOffset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
        start = Offset(offset, 0f),
        end = Offset(offset + 600f, 0f)
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun MonthlySummaryCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1040))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // "TOTAL SPENDING · APRIL" label shimmer
        ShimmerBox(modifier = Modifier.width(160.dp).height(12.dp))
        Spacer(Modifier.height(14.dp))
        // Big amount shimmer
        ShimmerBox(modifier = Modifier.width(200.dp).height(48.dp), cornerRadius = 10.dp)
        Spacer(Modifier.height(10.dp))
        // Subtitle shimmer
        ShimmerBox(modifier = Modifier.width(180.dp).height(14.dp))
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(3) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShimmerBox(modifier = Modifier.width(50.dp).height(10.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.width(60.dp).height(16.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        ShimmerBox(modifier = Modifier.width(130.dp).height(14.dp))
        Spacer(Modifier.height(12.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(10.dp), cornerRadius = 999.dp)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(4) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShimmerBox(modifier = Modifier.width(50.dp).height(10.dp))
                    Spacer(Modifier.height(5.dp))
                    ShimmerBox(modifier = Modifier.width(60.dp).height(10.dp))
                }
            }
        }
    }
}

@Composable
fun SpendingChartSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        ShimmerBox(modifier = Modifier.width(100.dp).height(14.dp))
        Spacer(Modifier.height(12.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(130.dp), cornerRadius = 12.dp)
    }
}

@Composable
fun TransactionItemSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(modifier = Modifier.size(40.dp), cornerRadius = 20.dp)
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp))
        }
        ShimmerBox(modifier = Modifier.width(60.dp).height(14.dp))
    }
}

@Composable
fun GenericCardSkeleton(lineCount: Int = 3, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShimmerBox(modifier = Modifier.width(120.dp).height(14.dp))
        repeat(lineCount) { i ->
            ShimmerBox(modifier = Modifier.fillMaxWidth(if (i % 2 == 0) 0.85f else 0.65f).height(12.dp))
        }
    }
}
