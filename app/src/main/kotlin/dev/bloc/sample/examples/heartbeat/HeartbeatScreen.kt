package dev.bloc.sample.examples.heartbeat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloc.sample.examples.calculator.LogEntry
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Colours
// ─────────────────────────────────────────────────────────────────────────────

private val HbGreen      = Color(0xFF4DDB9A)
private val HbNavy       = Color(0xFF0A0F19)
private val HbSurface    = Color(0xFF101825)
private val OrangeAccent = Color(0xFFFF9800)

// ─────────────────────────────────────────────────────────────────────────────
// Root
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartbeatScreen(
    showBackButton: Boolean,
    onBack: () -> Unit,
) {
    var sessionId by remember { mutableIntStateOf(0) }

    key(sessionId) {
        val bloc = remember { HeartbeatBloc() }

        LaunchedEffect(bloc) { bloc.send(HeartbeatEvent.Start) }
        DisposableEffect(bloc) { onDispose { if (!bloc.isClosed) bloc.close() } }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Heartbeat — Scoped Lifecycle",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = HbNavy),
                )
            },
            containerColor = HbNavy,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                LifecycleBanner()

                BoxWithConstraints(Modifier.fillMaxSize()) {
                    if (maxWidth >= 520.dp) {
                        val panelWidth = minOf(maxWidth * 0.48f, 380.dp)
                        Row(Modifier.fillMaxSize()) {
                            MonitorPanel(
                                bloc         = bloc,
                                modifier     = Modifier
                                    .width(panelWidth)
                                    .fillMaxHeight(),
                                onNewSession = { bloc.close(); sessionId++ },
                            )
                            VerticalDivider(color = Color.White.copy(alpha = 0.07f))
                            LifecycleLogPanel(bloc = bloc, modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            MonitorPanel(
                                bloc         = bloc,
                                modifier     = Modifier.fillMaxWidth().weight(0.52f),
                                onNewSession = { bloc.close(); sessionId++ },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                            LifecycleLogPanel(bloc = bloc, modifier = Modifier.fillMaxWidth().weight(0.48f))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Collapsible banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LifecycleBanner() {
    var expanded by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1A12)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("✕", fontSize = 18.sp, color = HbGreen.copy(alpha = 0.85f))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "close() — Lifecycle Management",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                )
                AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Text(
                        "This Bloc is NOT in BlocProvider — it is scoped to this screen via remember. " +
                            "Navigate away and it is closed automatically. " +
                            "Return to see a fresh Bloc start from zero. " +
                            "Tap New Session to close and recreate inline.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            IconButton(
                onClick  = { expanded = !expanded },
                modifier = Modifier.size(28.dp),
            ) {
                Text(if (expanded) "∧" else "∨", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
            }
        }
        HorizontalDivider(color = HbGreen.copy(alpha = 0.12f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monitor panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonitorPanel(
    bloc: HeartbeatBloc,
    modifier: Modifier = Modifier,
    onNewSession: () -> Unit,
) {
    val state    by bloc.stateFlow.collectAsState()
    val isClosed by bloc.closedFlow.collectAsState()

    Column(
        modifier = modifier
            .background(HbNavy)
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        PulseRing(
            tickCount = state.tickCount,
            isClosed  = isClosed,
            modifier  = Modifier.size(180.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (isClosed) "CLOSED" else state.formattedDuration,
            fontSize = 48.sp,
            fontWeight = FontWeight.Thin,
            fontFamily = FontFamily.Monospace,
            color = if (isClosed) OrangeAccent else Color.White,
        )
        Text(
            text = if (isClosed)
                "Navigate away to auto-close"
            else
                "${state.tickCount} tick${if (state.tickCount == 1) "" else "s"}",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.35f),
            fontFamily = FontFamily.Monospace,
        )

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HbSurface, RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ⓘ", fontSize = 12.sp, color = HbGreen.copy(alpha = 0.7f))
                Text("Scoped Bloc Pattern", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(0.6f))
            }
            Text(
                "This Bloc is not in BlocProvider. It is owned by the composable — created on entry, " +
                    "closed on exit. Navigate away to trigger close() automatically, or tap New Session below.",
                fontSize = 11.sp, color = Color.White.copy(0.35f), lineHeight = 16.sp, fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Brush.linearGradient(listOf(Color(0xFF4DA6FF), Color(0xFF1A66CC))))
                .clickable(
                    onClick = onNewSession,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .padding(horizontal = 28.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("New Session", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pulse ring
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PulseRing(tickCount: Int, isClosed: Boolean, modifier: Modifier = Modifier) {
    val accent   = if (isClosed) OrangeAccent else HbGreen
    val ripple1  = remember { Animatable(1f) }
    val ripple2  = remember { Animatable(1f) }
    val ripple3  = remember { Animatable(1f) }

    LaunchedEffect(tickCount) {
        if (isClosed) return@LaunchedEffect
        ripple1.snapTo(1f); ripple2.snapTo(1f); ripple3.snapTo(1f)
        coroutineScope {
            launch { ripple1.animateTo(1.5f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow)) }
            launch { delay(150); ripple2.animateTo(1.65f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow)) }
            launch { delay(300); ripple3.animateTo(1.8f,  spring(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow)) }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.size(160.dp).scale(ripple3.value).background(accent.copy(((1.8f - ripple3.value) / 0.8f).coerceIn(0f, 0.07f)), CircleShape))
        Box(Modifier.size(140.dp).scale(ripple2.value).background(accent.copy(((1.8f - ripple2.value) / 0.8f).coerceIn(0f, 0.11f)), CircleShape))
        Box(Modifier.size(120.dp).scale(ripple1.value).background(accent.copy(((1.8f - ripple1.value) / 0.8f).coerceIn(0f, 0.16f)), CircleShape))
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Brush.radialGradient(listOf(accent.copy(0.35f), accent.copy(0.06f))), CircleShape)
                .border(1.5.dp, accent.copy(0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isClosed) "✕" else "♡", fontSize = 32.sp, color = accent.copy(0.8f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lifecycle log panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LifecycleLogPanel(bloc: HeartbeatBloc, modifier: Modifier = Modifier) {
    val entries  by bloc.logFlow.collectAsState()
    val isClosed by bloc.closedFlow.collectAsState()

    Column(modifier = modifier.background(Color(0xFF080812))) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.025f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lifecycle Log", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    val accent = if (isClosed) OrangeAccent else HbGreen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(accent.copy(0.12f), RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Box(Modifier.size(5.dp).background(accent, CircleShape))
                        Text(if (isClosed) "CLOSED" else "ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accent, fontFamily = FontFamily.Monospace)
                    }
                }
                Text("${entries.size} events", fontSize = 10.sp, color = Color.White.copy(0.3f))
            }
            IconButton(onClick = { bloc.clearLog() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(18.dp))
            }
        }
        HorizontalDivider(color = Color.White.copy(0.07f))

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("♡", fontSize = 28.sp, color = Color.White.copy(0.12f))
                    Text("Starting…", fontSize = 14.sp, color = Color.White.copy(0.25f))
                }
            }
        } else {
            LazyColumn(reverseLayout = true, modifier = Modifier.fillMaxSize()) {
                items(entries.reversed(), key = { it.id.toString() }) { entry ->
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .background(if (entry.kind == LogEntry.Kind.CLOSE) OrangeAccent.copy(0.06f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .width(82.dp)
                                .background(entry.kind.color.copy(0.12f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 5.dp, vertical = 3.dp),
                        ) {
                            Text(entry.kind.icon, fontSize = 9.sp, color = entry.kind.color)
                            Text(entry.kind.label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = entry.kind.color, fontFamily = FontFamily.Monospace)
                        }
                        Text(entry.message, fontSize = 11.sp, color = Color.White.copy(0.75f), fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text(entry.timestampFormatted, fontSize = 9.sp, color = Color.White.copy(0.22f), fontFamily = FontFamily.Monospace)
                    }
                    HorizontalDivider(color = Color.White.copy(0.04f))
                }
            }
        }
    }
}
