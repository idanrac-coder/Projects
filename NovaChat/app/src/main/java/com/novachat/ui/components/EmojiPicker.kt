package com.novachat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class EmojiCategory(
    val name: String,
    val icon: String,
    val emojis: List<String>
)

private val emojiCategories = listOf(
    EmojiCategory("Smileys", "\uD83D\uDE00", listOf(
        "\uD83D\uDE00", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE01", "\uD83D\uDE06",
        "\uD83D\uDE05", "\uD83E\uDD23", "\uD83D\uDE02", "\uD83D\uDE42", "\uD83D\uDE43",
        "\uD83D\uDE09", "\uD83D\uDE0A", "\uD83D\uDE07", "\uD83E\uDD70", "\uD83D\uDE0D",
        "\uD83E\uDD29", "\uD83D\uDE18", "\uD83D\uDE17", "\uD83D\uDE1A", "\uD83D\uDE19",
        "\uD83E\uDD72", "\uD83D\uDE0B", "\uD83D\uDE1B", "\uD83D\uDE1C", "\uD83E\uDD2A",
        "\uD83D\uDE1D", "\uD83E\uDD11", "\uD83E\uDD17", "\uD83E\uDD2D", "\uD83E\uDD2B",
        "\uD83E\uDD14", "\uD83E\uDD10", "\uD83E\uDD28", "\uD83D\uDE10", "\uD83D\uDE11",
        "\uD83D\uDE36", "\uD83D\uDE0F", "\uD83D\uDE12", "\uD83D\uDE44", "\uD83D\uDE2C",
        "\uD83D\uDE25", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE31", "\uD83D\uDE33",
        "\uD83E\uDD7A", "\uD83D\uDE26", "\uD83D\uDE27", "\uD83D\uDE28", "\uD83D\uDE30",
        "\uD83D\uDE13", "\uD83D\uDE2B", "\uD83D\uDE29", "\uD83D\uDE24", "\uD83D\uDE21",
        "\uD83D\uDE20", "\uD83E\uDD2C", "\uD83D\uDE08", "\uD83D\uDC80", "\uD83D\uDCA9",
    )),
    EmojiCategory("Gestures", "\uD83D\uDC4D", listOf(
        "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4A", "\u270A", "\uD83E\uDD1B",
        "\uD83E\uDD1C", "\uD83D\uDC4F", "\uD83D\uDE4C", "\uD83D\uDC50", "\uD83E\uDD32",
        "\uD83E\uDD1D", "\uD83D\uDE4F", "\u270D\uFE0F", "\uD83D\uDC85", "\uD83E\uDD33",
        "\uD83D\uDCAA", "\uD83E\uDDB5", "\uD83E\uDDB6", "\uD83D\uDC42", "\uD83D\uDC43",
        "\u2764\uFE0F", "\uD83E\uDDE1", "\uD83D\uDC9B", "\uD83D\uDC9A", "\uD83D\uDC99",
        "\uD83D\uDC9C", "\uD83E\uDD0E", "\uD83D\uDDA4", "\uD83E\uDD0D", "\uD83D\uDC94",
        "\u2763\uFE0F", "\uD83D\uDC95", "\uD83D\uDC9E", "\uD83D\uDC93", "\uD83D\uDC97",
        "\uD83D\uDC96", "\uD83D\uDC98", "\uD83D\uDC9D", "\uD83D\uDC4B", "\u270B",
        "\uD83D\uDD90\uFE0F", "\u270C\uFE0F", "\uD83E\uDD1E", "\uD83E\uDD1F", "\uD83E\uDD18",
    )),
    EmojiCategory("Nature", "\uD83C\uDF3B", listOf(
        "\uD83C\uDF1E", "\uD83C\uDF1D", "\uD83C\uDF1B", "\uD83C\uDF1C", "\u2B50",
        "\uD83C\uDF1F", "\uD83C\uDF20", "\u2601\uFE0F", "\u26C5", "\u26C8\uFE0F",
        "\uD83C\uDF24\uFE0F", "\uD83C\uDF25\uFE0F", "\uD83C\uDF26\uFE0F", "\uD83C\uDF08",
        "\uD83C\uDF37", "\uD83C\uDF38", "\uD83C\uDF39", "\uD83C\uDF3A", "\uD83C\uDF3B",
        "\uD83C\uDF3C", "\uD83C\uDF31", "\uD83C\uDF32", "\uD83C\uDF33", "\uD83C\uDF34",
        "\uD83C\uDF35", "\uD83C\uDF3E", "\uD83C\uDF3F", "\u2618\uFE0F", "\uD83C\uDF40",
        "\uD83C\uDF41", "\uD83C\uDF42", "\uD83C\uDF43", "\uD83D\uDC36", "\uD83D\uDC31",
        "\uD83D\uDC2D", "\uD83D\uDC39", "\uD83D\uDC30", "\uD83E\uDD8A", "\uD83D\uDC3B",
        "\uD83D\uDC28", "\uD83D\uDC2F", "\uD83E\uDD81", "\uD83D\uDC2E", "\uD83D\uDC37",
    )),
    EmojiCategory("Food", "\uD83C\uDF54", listOf(
        "\uD83C\uDF4E", "\uD83C\uDF4F", "\uD83C\uDF4A", "\uD83C\uDF4B", "\uD83C\uDF4C",
        "\uD83C\uDF49", "\uD83C\uDF47", "\uD83C\uDF53", "\uD83E\uDED0", "\uD83C\uDF48",
        "\uD83C\uDF51", "\uD83C\uDF52", "\uD83C\uDF4D", "\uD83E\uDD5D", "\uD83C\uDF45",
        "\uD83C\uDF46", "\uD83E\uDD51", "\uD83C\uDF54", "\uD83C\uDF55", "\uD83C\uDF2D",
        "\uD83C\uDF2E", "\uD83C\uDF2F", "\uD83E\uDD6A", "\uD83E\uDDC0", "\uD83C\uDF56",
        "\uD83C\uDF57", "\uD83E\uDD69", "\uD83C\uDF73", "\uD83C\uDF5E", "\uD83E\uDD50",
        "\uD83E\uDD68", "\uD83C\uDF5F", "\uD83E\uDD5E", "\uD83E\uDDC7", "\uD83C\uDF70",
        "\uD83C\uDF82", "\uD83C\uDF67", "\uD83C\uDF68", "\uD83C\uDF69", "\uD83C\uDF6A",
    )),
    EmojiCategory("Activities", "\u26BD", listOf(
        "\u26BD", "\uD83C\uDFC0", "\uD83C\uDFC8", "\u26BE", "\uD83E\uDD4E",
        "\uD83C\uDFBE", "\uD83C\uDFD0", "\uD83C\uDFC9", "\uD83E\uDD4F", "\uD83C\uDFB1",
        "\uD83C\uDFD3", "\uD83C\uDFF8", "\uD83C\uDFD2", "\uD83E\uDD4D", "\uD83C\uDFAF",
        "\u26F3", "\uD83E\uDD4A", "\uD83E\uDD4B", "\uD83C\uDFC4", "\uD83C\uDFC7",
        "\uD83C\uDFCA", "\uD83D\uDEB4", "\uD83C\uDFC6", "\uD83C\uDFC5", "\uD83E\uDD47",
        "\uD83E\uDD48", "\uD83E\uDD49", "\uD83C\uDFAD", "\uD83C\uDFA8", "\uD83C\uDFAC",
        "\uD83C\uDFA4", "\uD83C\uDFA7", "\uD83C\uDFB5", "\uD83C\uDFB6", "\uD83C\uDFB9",
        "\uD83C\uDFAE", "\uD83C\uDFB0", "\uD83C\uDFB2", "\uD83C\uDFAF", "\uD83C\uDFB3",
    )),
    EmojiCategory("Objects", "\uD83D\uDCA1", listOf(
        "\uD83D\uDCF1", "\uD83D\uDCBB", "\u2328\uFE0F", "\uD83D\uDDA5\uFE0F",
        "\uD83D\uDCF7", "\uD83D\uDCF8", "\uD83D\uDD0D", "\uD83D\uDD0E", "\uD83D\uDCA1",
        "\uD83D\uDD26", "\uD83D\uDCDA", "\uD83D\uDCD6", "\uD83D\uDCD3", "\uD83D\uDCDD",
        "\u270F\uFE0F", "\uD83D\uDD8A\uFE0F", "\uD83D\uDCCC", "\uD83D\uDCCE", "\u2702\uFE0F",
        "\uD83D\uDCC1", "\uD83D\uDCC2", "\uD83D\uDCC5", "\uD83D\uDCC6", "\uD83D\uDCCA",
        "\uD83D\uDD11", "\uD83D\uDD12", "\uD83D\uDD13", "\uD83D\uDEA8", "\uD83D\uDD14",
        "\uD83C\uDF81", "\uD83C\uDF88", "\uD83C\uDF89", "\uD83C\uDF8A", "\u2709\uFE0F",
        "\uD83D\uDCE9", "\uD83D\uDCE8", "\uD83D\uDCE7", "\uD83D\uDCEC", "\uD83D\uDCED",
    )),
    EmojiCategory("Symbols", "\u2764\uFE0F", listOf(
        "\u2764\uFE0F", "\uD83D\uDC9B", "\uD83D\uDC9A", "\uD83D\uDC99", "\uD83D\uDC9C",
        "\u2714\uFE0F", "\u274C", "\u2753", "\u2755", "\u203C\uFE0F",
        "\u2049\uFE0F", "\uD83D\uDD1F", "\uD83D\uDD20", "\uD83D\uDD21", "\uD83D\uDD22",
        "\uD83D\uDD23", "\uD83D\uDD24", "\u2795", "\u2796", "\u2797",
        "\u267E\uFE0F", "\uD83D\uDCAF", "\uD83D\uDCA2", "\uD83D\uDCA5", "\uD83D\uDCAB",
        "\uD83D\uDCA6", "\uD83D\uDCA8", "\uD83D\uDD5B", "\uD83D\uDD50", "\uD83D\uDD51",
        "\uD83C\uDFF3\uFE0F", "\uD83C\uDFF4", "\uD83C\uDFF4\u200D\u2620\uFE0F",
        "\uD83C\uDDE6\uD83C\uDDEA", "\uD83C\uDDFA\uD83C\uDDF8", "\uD83C\uDDEC\uD83C\uDDE7",
    )),
)

@Composable
fun EmojiPicker(
    visible: Boolean,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        var selectedTab by remember { mutableIntStateOf(0) }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    edgePadding = 8.dp,
                    divider = {}
                ) {
                    emojiCategories.forEachIndexed { index, category ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(category.icon, fontSize = 20.sp) }
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(emojiCategories[selectedTab].emojis) { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable { onEmojiSelected(emoji) }
                                .padding(6.dp)
                                .size(36.dp)
                        )
                    }
                }
            }
        }
    }
}
