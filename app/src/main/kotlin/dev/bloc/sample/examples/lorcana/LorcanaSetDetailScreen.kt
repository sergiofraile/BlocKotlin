package dev.bloc.sample.examples.lorcana

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.bloc.sample.examples.lorcana.models.LorcanaCard

private val LorcPurpleSet = Color(0xFF9933E5)
private val LorcDarkSet   = Color(0xFF100C1E)
private val LorcSurfaceSet = Color(0xFF1A1230)

@Composable
fun LorcanaSetDetailScreen(
    setName: String,
    cachedCards: List<LorcanaCard>? = null,
    onCardsLoaded: (List<LorcanaCard>) -> Unit = {},
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onCardClick: (LorcanaCard) -> Unit,
) {
    var cards by remember { mutableStateOf(cachedCards) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(setName) {
        if (cards != null) return@LaunchedEffect
        isLoading = true
        errorMessage = null
        try {
            val result = LorcanaNetworkService().fetchCardsFromSet(setName)
            cards = result
            onCardsLoaded(result)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Network error"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(LorcDarkSet)) {
        if (showBackButton) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LorcDarkSet)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    setName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SetHeader(setName = setName, cardCount = cards?.size)

        when {
            isLoading -> SetLoadingView()
            errorMessage != null -> SetErrorView(message = errorMessage!!, onRetry = {
                cards = null
                errorMessage = null
            })
            cards?.isEmpty() == true -> SetEmptyView()
            cards != null -> CardsGrid(cards = cards!!, onCardClick = onCardClick)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SetHeader(setName: String, cardCount: Int?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(LorcPurpleSet.copy(0.12f), Color.Transparent)),
            )
            .padding(vertical = 28.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(listOf(LorcPurpleSet.copy(0.3f), Color.Transparent)),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(listOf(LorcPurpleSet, Color(0xFF6622BB))),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✨", fontSize = 22.sp)
                }
            }

            Text(
                "SET COLLECTION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = Color.Gray,
            )
            Text(
                setName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            cardCount?.let {
                Text(
                    "$it cards",
                    fontSize = 13.sp,
                    color = LorcPurpleSet.copy(0.8f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cards grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardsGrid(cards: List<LorcanaCard>, onCardClick: (LorcanaCard) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(cards, key = { it.id }) { card ->
            CardGridItem(card = card, onClick = { onCardClick(card) })
        }
    }
}

@Composable
private fun CardGridItem(card: LorcanaCard, onClick: () -> Unit) {
    val inkColor = inkColorOf(card)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LorcSurfaceSet)
            .border(1.dp, inkColor.copy(0.2f), RoundedCornerShape(12.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(6.dp),
    ) {
        if (card.image != null) {
            AsyncImage(
                model = card.image,
                contentDescription = card.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.714f)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.714f)
                    .background(
                        Brush.linearGradient(listOf(inkColor, inkColor.copy(0.5f))),
                        RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(inkIconForColor(card.inkColor), fontSize = 18.sp)
            }
        }

        Text(
            card.name,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
        )
    }
}

private fun inkIconForColor(inkColor: dev.bloc.sample.examples.lorcana.models.InkColor): String = when (inkColor) {
    dev.bloc.sample.examples.lorcana.models.InkColor.AMBER     -> "🟡"
    dev.bloc.sample.examples.lorcana.models.InkColor.AMETHYST  -> "💜"
    dev.bloc.sample.examples.lorcana.models.InkColor.EMERALD   -> "💚"
    dev.bloc.sample.examples.lorcana.models.InkColor.RUBY      -> "❤️"
    dev.bloc.sample.examples.lorcana.models.InkColor.SAPPHIRE  -> "💙"
    dev.bloc.sample.examples.lorcana.models.InkColor.STEEL     -> "⚙️"
    dev.bloc.sample.examples.lorcana.models.InkColor.UNKNOWN   -> "⚪"
}

// ─────────────────────────────────────────────────────────────────────────────
// Card-back placeholder — shown in Extra pane when no card is selected
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LorcanaSetCardPlaceholder() {
    val gold    = Color(0xFFD4AF37)
    val goldDim = Color(0xFF8B6914)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF100C1E)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            // Lorcana card-back artwork
            Box(
                modifier = Modifier
                    .size(width = 148.dp, height = 207.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF090720), Color(0xFF120C38), Color(0xFF090720))),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(9.dp)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(gold, goldDim, gold, goldDim, gold)),
                            shape = RoundedCornerShape(10.dp),
                        ),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("·  ·  ·", color = gold.copy(0.45f), fontSize = 9.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(
                                Brush.radialGradient(listOf(gold.copy(0.18f), Color.Transparent)),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    Brush.linearGradient(listOf(gold.copy(0.22f), goldDim.copy(0.12f))),
                                    CircleShape,
                                )
                                .border(1.dp, gold.copy(0.35f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🪄", fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "DISNEY",
                        color = gold.copy(0.55f),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    )
                    Text(
                        "LORCANA",
                        color = gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 5.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("·  ·  ·", color = gold.copy(0.45f), fontSize = 9.sp, letterSpacing = 2.sp)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Select a card",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Tap any card in the set to reveal its details",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading / error / empty
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SetLoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(color = LorcPurpleSet)
            Text("Loading set cards…", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SetErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("⚠️", fontSize = 40.sp)
            Text("Failed to load set", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(message, color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(LorcPurpleSet)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onRetry)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SetEmptyView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("🃏", fontSize = 40.sp)
            Text("No cards found", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("This set doesn't have any cards yet", color = Color.Gray, fontSize = 13.sp)
        }
    }
}
