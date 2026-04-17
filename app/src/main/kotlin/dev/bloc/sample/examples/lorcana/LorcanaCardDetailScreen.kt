package dev.bloc.sample.examples.lorcana

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import dev.bloc.sample.examples.lorcana.models.LorcanaCard

@Composable
fun LorcanaCardDetailScreen(
    card: LorcanaCard,
    onNavigateToSet: (String) -> Unit,
) {
    val inkColor = inkColorOf(card)
    var isCardExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF100C1E))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Card image section
            CardImageSection(
                card = card,
                inkColor = inkColor,
                onTap = { isCardExpanded = true },
            )

            // Stats row — all boxes equal width
            val hasStats = card.cost != null || card.strength != null || card.willpower != null || card.lore != null
            if (hasStats) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    card.cost?.let      { StatBlock("💧", "Cost",      "$it", Modifier.weight(1f)) }
                    card.strength?.let  { StatBlock("⚡", "Strength",  "$it", Modifier.weight(1f)) }
                    card.willpower?.let { StatBlock("🛡", "Willpower", "$it", Modifier.weight(1f)) }
                    card.lore?.let      { StatBlock("⭐", "Lore",      "$it", Modifier.weight(1f)) }
                }
            }

            HorizontalDivider(color = Color.White.copy(0.08f))

            // Card info section
            InfoSection("Card Info") {
                InfoRow("Rarity",  card.rarity ?: "—")
                InfoRow("Type",    card.type ?: "—")
                InfoRow("Color",   card.color?.replaceFirstChar { it.uppercase() } ?: "—")
                InfoRow("Number",  card.cardNum?.let { "#$it" } ?: "—")
                InfoRow("Inkable", if (card.inkable == true) "Yes" else "No")
                card.artist?.let { InfoRow("Artist", it) }
                card.franchises?.let { if (it.isNotEmpty()) InfoRow("Franchise", it) }
            }

            // Tappable set section
            card.setName?.let { setName ->
                SetRow(
                    setName = setName,
                    inkColor = inkColor,
                    onClick = { onNavigateToSet(setName) },
                )
            }

            // Abilities
            card.abilities?.let {
                InfoSection("Abilities") {
                    Text(it, fontSize = 13.sp, color = Color.White.copy(0.75f), lineHeight = 20.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Body text
            card.bodyText?.let {
                InfoSection("Body Text") {
                    Text(it, fontSize = 13.sp, color = Color.White.copy(0.65f), lineHeight = 20.sp)
                }
            }

            // Flavor text
            card.flavorText?.let {
                InfoSection("Flavor Text") {
                    Text(
                        "\"$it\"",
                        fontSize = 13.sp, color = inkColor.copy(0.7f), lineHeight = 20.sp,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        // Full-screen image overlay — sits on top of everything
        AnimatedVisibility(
            visible = isCardExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { isCardExpanded = false },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = isCardExpanded,
                    enter = scaleIn(initialScale = 0.7f) + fadeIn(),
                    exit = scaleOut(targetScale = 0.7f) + fadeOut(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        if (card.image != null) {
                            SubcomposeAsyncImage(
                                model = card.image,
                                contentDescription = card.name,
                                modifier = Modifier
                                    .widthIn(max = 380.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Fit,
                            ) {
                                when (painter.state) {
                                    is AsyncImagePainter.State.Loading -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.714f)
                                                .background(inkColor.copy(0.3f), RoundedCornerShape(20.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(color = inkColor, modifier = Modifier.size(40.dp))
                                        }
                                    }
                                    is AsyncImagePainter.State.Error -> {
                                        CardPlaceholderLarge(inkColor = inkColor)
                                    }
                                    else -> SubcomposeAsyncImageContent()
                                }
                            }
                        } else {
                            CardPlaceholderLarge(inkColor = inkColor)
                        }
                        Text(
                            "Tap anywhere to close",
                            fontSize = 12.sp,
                            color = Color.White.copy(0.45f),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card image section (tappable to expand)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardImageSection(
    card: LorcanaCard,
    inkColor: Color,
    onTap: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onTap,
                ),
        ) {
            if (card.image != null) {
                SubcomposeAsyncImage(
                    model = card.image,
                    contentDescription = card.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Fit,
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.714f)
                                    .background(
                                        Brush.linearGradient(listOf(inkColor.copy(0.4f), Color(0xFF100C1E))),
                                        RoundedCornerShape(20.dp),
                                    )
                                    .border(1.dp, inkColor.copy(0.35f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = inkColor, modifier = Modifier.size(36.dp))
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            CardHeroFallback(card = card, inkColor = inkColor)
                        }
                        else -> SubcomposeAsyncImageContent()
                    }
                }
            } else {
                CardHeroFallback(card = card, inkColor = inkColor)
            }
        }

        Text(
            "Tap to enlarge",
            fontSize = 11.sp,
            color = Color.White.copy(0.35f),
        )
    }
}

@Composable
private fun CardHeroFallback(card: LorcanaCard, inkColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.linearGradient(listOf(inkColor.copy(0.4f), Color(0xFF100C1E))),
                RoundedCornerShape(20.dp),
            )
            .border(1.dp, inkColor.copy(0.35f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(inkIcon2(card), fontSize = 48.sp)
            Text(card.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            card.classifications?.let {
                Text(it, fontSize = 12.sp, color = inkColor.copy(0.8f))
            }
        }
    }
}

@Composable
private fun CardPlaceholderLarge(inkColor: Color) {
    Box(
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .aspectRatio(0.714f)
            .background(
                Brush.linearGradient(listOf(inkColor.copy(0.5f), inkColor.copy(0.2f))),
                RoundedCornerShape(20.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text("🃏", fontSize = 60.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stat block — accepts a Modifier so callers can apply weight(1f)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatBlock(icon: String, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(Color.White.copy(0.04f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(icon, fontSize = 18.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Set row — tappable, navigates to set detail
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SetRow(setName: String, inkColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(listOf(inkColor.copy(0.15f), Color.White.copy(0.03f))),
            )
            .border(1.dp, inkColor.copy(0.3f), RoundedCornerShape(14.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "SET",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color.Gray,
            )
            Text(
                setName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
        Text("›", fontSize = 22.sp, color = inkColor)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info section / row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color.Gray)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.03f), RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(0.07f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = Color.White.copy(0.85f), fontFamily = FontFamily.Monospace)
    }
}

private fun inkIcon2(card: LorcanaCard): String = when (card.inkColor) {
    dev.bloc.sample.examples.lorcana.models.InkColor.AMBER     -> "🟡"
    dev.bloc.sample.examples.lorcana.models.InkColor.AMETHYST  -> "💜"
    dev.bloc.sample.examples.lorcana.models.InkColor.EMERALD   -> "💚"
    dev.bloc.sample.examples.lorcana.models.InkColor.RUBY      -> "❤️"
    dev.bloc.sample.examples.lorcana.models.InkColor.SAPPHIRE  -> "💙"
    dev.bloc.sample.examples.lorcana.models.InkColor.STEEL     -> "⚙️"
    dev.bloc.sample.examples.lorcana.models.InkColor.UNKNOWN   -> "⚪"
}
