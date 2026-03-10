package dev.bloc.sample.examples.lorcana

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.bloc.compose.BlocSelector
import dev.bloc.sample.examples.lorcana.models.InkColor
import dev.bloc.sample.examples.lorcana.models.LorcanaCard
import dev.bloc.sample.examples.lorcana.models.PaginationSummary
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// Colours
// ─────────────────────────────────────────────────────────────────────────────

private val LorcPurple  = Color(0xFF9933E5)
private val LorcDark    = Color(0xFF14101E)
private val LorcMid     = Color(0xFF1E163A)
private val LorcSurface = Color(0xFF1A1230)
private val LorcBorder  = Color(0xFF2E2050)

// ─────────────────────────────────────────────────────────────────────────────
// Navigation back stack
// ─────────────────────────────────────────────────────────────────────────────

sealed class LorcanaRoute {
    data object List : LorcanaRoute()
    data class CardDetail(val card: LorcanaCard) : LorcanaRoute()
    data class SetDetail(val setName: String) : LorcanaRoute()
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LorcanaScreen(
    bloc: LorcanaBloc,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onNavigateToSet: ((String) -> Unit)? = null,
) {
    val state by bloc.stateFlow.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var backStack by remember { mutableStateOf<kotlin.collections.List<LorcanaRoute>>(listOf(LorcanaRoute.List)) }
    val setCardCache = remember { mutableStateMapOf<String, List<LorcanaCard>>() }

    fun pop() {
        if (backStack.size > 1) backStack = backStack.dropLast(1) else onBack()
    }

    val currentRoute = backStack.last()

    BackHandler(enabled = backStack.size > 1 || showBackButton) { pop() }

    val title = when (val r = currentRoute) {
        is LorcanaRoute.List       -> "Lorcana Cards"
        is LorcanaRoute.CardDetail -> r.card.name
        is LorcanaRoute.SetDetail  -> r.setName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (backStack.size > 1 || showBackButton) {
                        IconButton(onClick = { pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LorcDark),
            )
        },
        containerColor = LorcDark,
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(LorcDark, LorcMid, LorcDark))),
            )
            StarfieldCanvas(modifier = Modifier.fillMaxSize())

            when (val route = currentRoute) {
                is LorcanaRoute.List -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchBar(
                            searchText = searchText,
                            onSearch = { text ->
                                searchText = text
                                if (text.isEmpty()) bloc.send(LorcanaEvent.Clear)
                                else bloc.send(LorcanaEvent.Search(text))
                            },
                            onFetchAll = {
                                searchText = ""
                                bloc.send(LorcanaEvent.FetchAll)
                            },
                        )
                        when {
                            state.isLoading && state.cards.isEmpty() -> LoadingView()
                            state.error != null && state.cards.isEmpty() ->
                                ErrorView(message = state.error!!.message, bloc = bloc)
                            state.cards.isEmpty() && !state.isSearching -> InitialView()
                            state.cards.isEmpty() && state.isSearching  -> NoResultsView()
                            else -> CardsListView(
                                bloc = bloc,
                                state = state,
                                onCardClick = { card -> backStack = backStack + LorcanaRoute.CardDetail(card) },
                            )
                        }
                    }
                }

                is LorcanaRoute.CardDetail -> {
                    LorcanaCardDetailScreen(
                        card = route.card,
                        onNavigateToSet = { setName ->
                            if (onNavigateToSet != null) onNavigateToSet(setName)
                            else backStack = backStack + LorcanaRoute.SetDetail(setName)
                        },
                    )
                }

                is LorcanaRoute.SetDetail -> {
                    LorcanaSetDetailScreen(
                        setName       = route.setName,
                        cachedCards   = setCardCache[route.setName],
                        onCardsLoaded = { loaded -> setCardCache[route.setName] = loaded },
                        onCardClick   = { card -> backStack = backStack + LorcanaRoute.CardDetail(card) },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Starfield canvas
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StarfieldCanvas(modifier: Modifier = Modifier) {
    val stars = remember {
        (0..35).map { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 0.35f + 0.05f) }
    }
    Canvas(modifier = modifier) {
        stars.forEach { (x, y, alpha) ->
            val radius = if (alpha > 0.25f) 2f else 1f
            drawCircle(Color.White.copy(alpha = alpha), radius = radius, center = Offset(x * size.width, y * size.height))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(searchText: String, onSearch: (String) -> Unit, onFetchAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value         = searchText,
            onValueChange = onSearch,
            modifier      = Modifier.weight(1f),
            singleLine    = true,
            placeholder   = { Text("Search cards… (min 3 chars)", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon   = { Text("🔍", fontSize = 14.sp) },
            trailingIcon  = if (searchText.isNotEmpty()) {
                { IconButton(onClick = { onSearch("") }) { Text("✕", fontSize = 14.sp, color = Color.Gray) } }
            } else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = LorcPurple,
                unfocusedBorderColor = LorcBorder,
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = LorcPurple,
            ),
            shape = RoundedCornerShape(14.dp),
        )
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF9933E5), Color(0xFF6622BB))))
                .clickable(onClick = onFetchAll, indication = null, interactionSource = remember { MutableInteractionSource() }),
            contentAlignment = Alignment.Center,
        ) {
            Text("✨", fontSize = 20.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State views
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InitialView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Brush.radialGradient(listOf(LorcPurple.copy(0.3f), Color.Transparent)), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Brush.linearGradient(listOf(LorcPurple, Color(0xFF6622BB))), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("🪄", fontSize = 38.sp) }
        }
        Spacer(Modifier.height(28.dp))
        Text("DISNEY", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp, color = Color.Gray)
        Spacer(Modifier.height(6.dp))
        Text("Lorcana", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Card Collection", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        Text(
            "Search for cards or tap ✨ to browse all cards",
            fontSize = 14.sp, color = Color.Gray, fontFamily = FontFamily.Default,
        )
    }
}

@Composable
private fun LoadingView() {
    val transition = rememberInfiniteTransition(label = "spin")
    val rotation by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(50.dp).rotate(rotation), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(50.dp)) {
                drawCircle(Color.Gray.copy(0.2f), style = Stroke(4.dp.toPx()))
                drawArc(
                    brush = Brush.linearGradient(listOf(LorcPurple, Color(0xFFE040FB))),
                    startAngle = -90f, sweepAngle = 108f, useCenter = false,
                    style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Summoning cards…", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
private fun ErrorView(message: String, bloc: LorcanaBloc) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⚠️", fontSize = 44.sp)
        Spacer(Modifier.height(16.dp))
        Text("Something went wrong", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(LorcPurple)
                .clickable(onClick = { bloc.send(LorcanaEvent.FetchAll) }, indication = null, interactionSource = remember { MutableInteractionSource() })
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text("Try Again", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun NoResultsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🔍", fontSize = 40.sp)
        Spacer(Modifier.height(16.dp))
        Text("No cards found", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Try a different search term", color = Color.Gray, fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cards list with infinite scroll
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardsListView(
    bloc: LorcanaBloc,
    state: LorcanaState,
    onCardClick: (LorcanaCard) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - 5
        }.collect { nearBottom ->
            val s = bloc.state
            if (nearBottom && s.hasMorePages && !s.isLoadingMore && !s.isLoading) {
                bloc.send(LorcanaEvent.LoadNextPage)
            }
        }
    }

    LazyColumn(
        state           = listState,
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier        = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(state.cards, key = { _, c -> c.id }) { _, card ->
            CardRow(card = card, onClick = { onCardClick(card) })
        }

        item(key = "footer-loading") {
            BlocSelector(
                bloc     = bloc,
                selector = { it.isLoadingMore },
            ) { isLoadingMore ->
                if (isLoadingMore) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("✨", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Loading more…", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }

        item(key = "footer-end") {
            BlocSelector(
                bloc     = bloc,
                selector = { PaginationSummary(hasMore = it.hasMorePages, count = it.cards.size) },
            ) { summary ->
                if (!summary.hasMore && summary.count > 0) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "You've seen all ${summary.count} cards! ✨",
                            color = Color.Gray, fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CardRow(card: LorcanaCard, onClick: () -> Unit) {
    val inkColor = inkColorOf(card)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LorcSurface)
            .border(1.dp, Brush.linearGradient(listOf(inkColor.copy(0.3f), Color.Transparent)), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick, indication = null, interactionSource = remember { MutableInteractionSource() })
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Card image thumbnail — falls back to ink color placeholder
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 72.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            if (card.image != null) {
                AsyncImage(
                    model = card.image,
                    contentDescription = card.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(inkColor, inkColor.copy(0.5f)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(inkIcon(card.inkColor), fontSize = 20.sp)
                }
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                card.name,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                card.cost?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("💧", fontSize = 9.sp)
                        Text("$it", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = inkColor)
                    }
                }
                card.type?.let { Text(it, fontSize = 10.sp, color = Color.Gray) }
                card.rarity?.let { Text("• $it", fontSize = 10.sp, color = Color.Gray) }
            }
            card.setName?.let {
                Text(it, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = LorcPurple.copy(0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            card.strength?.let  { StatChip("⚡", it, Color(0xFFFF9800)) }
            card.willpower?.let { StatChip("🛡", it, Color(0xFF42A5F5)) }
            card.lore?.let      { StatChip("⭐", it, Color(0xFFFFEB3B)) }
        }

        Text("›", fontSize = 18.sp, color = Color.Gray.copy(0.4f))
    }
}

@Composable
private fun StatChip(icon: String, value: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(icon, fontSize = 9.sp)
        Text("$value", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

fun inkColorOf(card: LorcanaCard): Color = when (card.inkColor) {
    InkColor.AMBER     -> Color(0xFFFFBF33)
    InkColor.AMETHYST  -> Color(0xFF9933E5)
    InkColor.EMERALD   -> Color(0xFF33C260)
    InkColor.RUBY      -> Color(0xFFE53333)
    InkColor.SAPPHIRE  -> Color(0xFF3366E5)
    InkColor.STEEL     -> Color(0xFF9999A8)
    InkColor.UNKNOWN   -> Color.Gray
}

private fun inkIcon(inkColor: InkColor): String = when (inkColor) {
    InkColor.AMBER     -> "🟡"
    InkColor.AMETHYST  -> "💜"
    InkColor.EMERALD   -> "💚"
    InkColor.RUBY      -> "❤️"
    InkColor.SAPPHIRE  -> "💙"
    InkColor.STEEL     -> "⚙️"
    InkColor.UNKNOWN   -> "⚪"
}
