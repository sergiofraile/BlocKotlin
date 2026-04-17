package dev.bloc.sample.examples.formulaone

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloc.sample.examples.formulaone.models.DriverChampionship
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember

// ─────────────────────────────────────────────────────────────────────────────
// Colours
// ─────────────────────────────────────────────────────────────────────────────

private val F1Red     = Color(0xFFE31018)
private val F1Dark    = Color(0xFF141416)
private val F1Surface = Color(0xFF1E1E22)
private val F1Border  = Color(0xFF2E2E34)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormulaOneScreen(
    bloc: FormulaOneBloc,
    showBackButton: Boolean,
    onBack: () -> Unit,
) {
    val state by bloc.stateFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver's Championship", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = F1Dark),
            )
        },
        containerColor = F1Dark,
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Subtle diagonal racing stripes background
            CheckeredStripes(modifier = Modifier.fillMaxSize())

            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "f1state",
            ) { currentState ->
                when (currentState) {
                    is FormulaOneState.Initial -> InitialView(bloc = bloc)
                    is FormulaOneState.Loading -> LoadingView()
                    is FormulaOneState.Loaded  -> DriversListView(drivers = currentState.drivers, bloc = bloc)
                    is FormulaOneState.Error   -> ErrorView(message = currentState.message, bloc = bloc)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Checkered stripe background (canvas-drawn diagonal stripes like iOS)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CheckeredStripes(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stripeWidth = 40.dp.toPx()
        val stripeColor = Color.White.copy(alpha = 0.022f)
        var x = 0f
        while (x < size.width + 200.dp.toPx()) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(x, 0f)
                lineTo(x - 100.dp.toPx(), size.height)
                lineTo(x - 100.dp.toPx() + stripeWidth, size.height)
                lineTo(x + stripeWidth, 0f)
                close()
            }
            drawPath(path, stripeColor)
            x += stripeWidth * 2
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Initial state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InitialView(bloc: FormulaOneBloc) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // F1 badge
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFE31018), Color(0xFFB30010))),
                    RoundedCornerShape(50),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("F1", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Default)
        }

        Spacer(Modifier.height(32.dp))

        Text("FORMULA 1", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 7.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Text("Driver's Championship", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Spacer(Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(Color(0xFFE31018), Color(0xFFBF0010))))
                .clickable(
                    onClick = { bloc.send(FormulaOneEvent.LoadChampionship) },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .padding(horizontal = 36.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🏁", fontSize = 18.sp)
                Text("Load Championship", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
            Canvas(Modifier.size(60.dp)) {
                drawCircle(Color.Gray.copy(0.25f), style = Stroke(4.dp.toPx()))
                drawArc(
                    brush = Brush.linearGradient(listOf(F1Red, Color(0xFFFF6B35))),
                    startAngle = rotation - 90f,
                    sweepAngle = 108f,
                    useCenter = false,
                    style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Loading Championship...", color = Color.Gray, fontSize = 15.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drivers list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DriversListView(drivers: List<DriverChampionship>, bloc: FormulaOneBloc) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = { bloc.send(FormulaOneEvent.Clear) },
                modifier = Modifier.size(32.dp),
            ) {
                Text("✕", color = Color.Gray, fontSize = 16.sp)
            }
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(drivers, key = { _, d -> d.classificationId }) { index, driver ->
                DriverCard(driver = driver, position = index + 1)
            }
        }
    }
}

@Composable
private fun DriverCard(driver: DriverChampionship, position: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(F1Surface, RoundedCornerShape(16.dp))
            .border(1.dp, F1Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Position badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(positionGradient(position), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$position", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Driver info
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${driver.driver.name} ${driver.driver.surname}",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("#${driver.driver.number}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Cyan, fontFamily = FontFamily.Monospace)
                Text("•", color = Color.Gray.copy(0.5f))
                Text(driver.team.teamName, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Points
        Column(horizontalAlignment = Alignment.End) {
            Text("${driver.points}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("PTS", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, letterSpacing = 1.sp)
        }
    }
}

private fun positionGradient(position: Int): Brush = when (position) {
    1    -> Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFDAA520)))
    2    -> Brush.linearGradient(listOf(Color(0xFFC0C0C8), Color(0xFF8C8C99)))
    3    -> Brush.linearGradient(listOf(Color(0xFFCC8033), Color(0xFF995922)))
    else -> Brush.linearGradient(listOf(Color(0xFF404048), Color(0xFF333339)))
}

// ─────────────────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorView(message: String, bloc: FormulaOneBloc) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Something went wrong", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(message, fontSize = 13.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(F1Red)
                .clickable(
                    onClick = { bloc.send(FormulaOneEvent.LoadChampionship) },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .padding(horizontal = 28.dp, vertical = 12.dp),
        ) {
            Text("Try Again", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
