package com.novachat.ui.themes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.R
import com.novachat.domain.model.ConversationBackground
import com.novachat.domain.model.WallpaperType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundsScreen(
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit = {},
    viewModel: BackgroundsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.conversation_backgrounds_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.backgrounds, key = { it.id }) { background ->
                    BackgroundCard(
                        background = background,
                        isSelected = background.id == uiState.selectedId,
                        onClick = {
                            if (uiState.isPremium) {
                                viewModel.showPreview(background)
                            } else {
                                onNavigateToPremium()
                            }
                        }
                    )
                }
            }

            if (!uiState.isPremium) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.backgrounds_premium_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onNavigateToPremium) {
                            Text(stringResource(R.string.upgrade_to_premium))
                        }
                    }
                }
            }
        }

        uiState.previewBackground?.let { bg ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissPreview() },
                title = { Text(stringResource(R.string.preview_prefix) + bg.displayName) },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(backgroundBrush(bg), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (bg.type == WallpaperType.IMAGE && bg.imageResName != null) {
                            val imageResId = context.resources.getIdentifier(
                                bg.imageResName,
                                "drawable",
                                context.packageName
                            )
                            if (imageResId != 0) {
                                Image(
                                    painter = painterResource(imageResId),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.9f
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Hi there!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        "Hello! How are you?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                        RoundedCornerShape(20.dp)
                                    )
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.confirmPreview() }) {
                        Text(stringResource(R.string.apply))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissPreview() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun BackgroundCard(
    background: ConversationBackground,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderModifier = if (isSelected) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
    } else Modifier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .then(borderModifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush(background))
                    .padding(12.dp)
            ) {
            }
            Text(
                text = background.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (background.id == "default") MaterialTheme.colorScheme.onSurface
                       else textColorForBackground(background),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun backgroundBrush(background: ConversationBackground): Brush {
    return when {
        background.id == "default" -> Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
        )
        background.type == WallpaperType.GRADIENT || background.type == WallpaperType.IMAGE -> Brush.verticalGradient(
            colors = listOf(
                background.primaryColorCompose,
                background.secondaryColorCompose
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(background.primaryColorCompose, background.primaryColorCompose)
        )
    }
}

private fun textColorForBackground(background: ConversationBackground): Color {
    if (background.id == "default") return Color.Unspecified
    val c = background.primaryColorCompose
    val lum = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
    return if (lum > 0.5f) Color(0xFF1A1B2E) else Color(0xFFE8E8F0)
}
