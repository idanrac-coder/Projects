package com.novachat.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.domain.model.BubbleShape

private val colorPalette = listOf(
    0xFFD32F2F, 0xFFC2185B, 0xFF7B1FA2, 0xFF512DA8, 0xFF303F9F,
    0xFF1976D2, 0xFF0288D1, 0xFF0097A7, 0xFF00796B, 0xFF388E3C,
    0xFF689F38, 0xFFAFB42B, 0xFFFBC02D, 0xFFFFA000, 0xFFF57C00,
    0xFFE64A19, 0xFF5D4037, 0xFF616161, 0xFF455A64, 0xFF000000,
    0xFFFFFFFF, 0xFFFFF9C4, 0xFFE8F5E9, 0xFFE3F2FD, 0xFFFCE4EC,
    0xFFEDE7F6, 0xFFFFF3E0, 0xFFE0F2F1, 0xFFF3E5F5, 0xFFEFEBE9,
    0xFF6750A4, 0xFFE8DEF8, 0xFF1D1B20, 0xFFFFFBFE, 0xFF625B71,
    0xFF00E5FF, 0xFFFF00FF, 0xFF76FF03, 0xFFFF1744, 0xFFFFD600
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeEditorScreen(
    onBack: () -> Unit,
    viewModel: ThemeEditorViewModel = hiltViewModel()
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.saveTheme() }) {
                Icon(Icons.Default.Save, contentDescription = "Save theme")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Preview
            Text(
                text = "Live Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(theme.backgroundColor))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                            .background(Color(theme.receivedBubbleColor))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Hey, how are you?", color = Color(theme.receivedTextColor), style = MaterialTheme.typography.bodyMedium)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                            .background(Color(theme.sentBubbleColor))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("I'm doing great! Thanks for asking.", color = Color(theme.sentTextColor), style = MaterialTheme.typography.bodyMedium)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                            .background(Color(theme.receivedBubbleColor))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("That's wonderful!", color = Color(theme.receivedTextColor), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Theme Name
            OutlinedTextField(
                value = theme.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Theme Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Bubble Shape
            Text("Bubble Shape", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BubbleShape.entries.forEach { shape ->
                    FilterChip(
                        selected = theme.bubbleShape == shape,
                        onClick = { viewModel.updateBubbleShape(shape) },
                        label = { Text(shape.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        leadingIcon = if (theme.bubbleShape == shape) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Color Sections
            ColorPickerSection("Primary Color", theme.primaryColor) { viewModel.updatePrimaryColor(it) }
            ColorPickerSection("Secondary Color", theme.secondaryColor) { viewModel.updateSecondaryColor(it) }
            ColorPickerSection("Background", theme.backgroundColor) { viewModel.updateBackgroundColor(it) }
            ColorPickerSection("Surface", theme.surfaceColor) { viewModel.updateSurfaceColor(it) }
            ColorPickerSection("Sent Bubble", theme.sentBubbleColor) { viewModel.updateSentBubbleColor(it) }
            ColorPickerSection("Received Bubble", theme.receivedBubbleColor) { viewModel.updateReceivedBubbleColor(it) }
            ColorPickerSection("Sent Text", theme.sentTextColor) { viewModel.updateSentTextColor(it) }
            ColorPickerSection("Received Text", theme.receivedTextColor) { viewModel.updateReceivedTextColor(it) }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerSection(
    label: String,
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(selectedColor))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            colorPalette.forEach { color ->
                val isSelected = color == selectedColor
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        .clickable { onColorSelected(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (color == 0xFFFFFFFF.toLong() || color == 0xFFFFF9C4.toLong())
                                Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
