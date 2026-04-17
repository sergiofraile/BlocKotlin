package dev.bloc.sample.examples.scoreboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloc.compose.BlocBuilder
import dev.bloc.compose.BlocConsumer
import dev.bloc.compose.BlocListener
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Tier
// ─────────────────────────────────────────────────────────────────────────────

private enum class Tier(val label: String) {
    BRONZE("Bronze"), SILVER("Silver"), GOLD("Gold"), PLATINUM("Platinum");

    val color: Color get() = when (this) {
        BRONZE   -> Color(0xFFCC7033)
        SILVER   -> Color(0xFFBEBEC8)
        GOLD     -> Color(0xFFF2C000)
        PLATINUM -> Color(0xFF4CE6F0)
    }
    val gradientColors: List<Color> get() = when (this) {
        BRONZE   -> listOf(Color(0xFFFFDA99), Color(0xFFB25708))
        SILVER   -> listOf(Color(0xFFFFFFFF), Color(0xFF8C8E9E))
        GOLD     -> listOf(Color(0xFFFFF280), Color(0xFFD98B00))
        PLATINUM -> listOf(Color(0xFFBFFEFF), Color(0xFF1AA6E6))
    }
    val icon: String get() = when (this) {
        BRONZE   -> "🥉"
        SILVER   -> "🥈"
        GOLD     -> "🏆"
        PLATINUM -> "👑"
    }
    val subtitle: String get() = when (this) {
        BRONZE   -> "Reach 10 pts to advance"
        SILVER   -> "Reach 20 pts to advance"
        GOLD     -> "Reach 30 pts to advance"
        PLATINUM -> "Maximum tier reached"
    }

    companion object {
        fun of(score: Int): Tier = when {
            score < 10 -> BRONZE
            score < 20 -> SILVER
            score < 30 -> GOLD
            else       -> PLATINUM
        }
    }
}

private fun nextMilestone(score: Int): Int = score + (5 - score % 5)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreScreen(
    bloc: ScoreBloc,
    showBackButton: Boolean,
    onBack: () -> Unit,
) {
    var milestoneText by remember { mutableStateOf<String?>(null) }
    var tierPulse     by remember { mutableStateOf(false) }

    val bgTop    = Color(0xFF0F0F1A)
    val bgBottom = Color(0xFF1A1428)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Score Board", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgTop),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(bgTop, bgBottom))),
        ) {
            // ── 1. BlocListener — fires milestone side-effects only, no rebuild ──
            BlocListener(
                bloc       = bloc,
                listenWhen = { _, new -> new > 0 && new % 5 == 0 },
                listener   = { score ->
                    milestoneText = "🎯 $score points!"
                },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Milestone toast — lives in the layout flow so it never overlaps the badges
                    AnimatedVisibility(
                        visible = milestoneText != null,
                        enter = slideInVertically { -it } + fadeIn() + expandVertically(expandFrom = Alignment.Top),
                        exit  = slideOutVertically { -it } + fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            milestoneText?.let { MilestoneBanner(text = it) }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        FeatureBadgeRow()

                        // ── 2. BlocBuilder — score numeral updates on every point ──
                        BlocBuilder(bloc = bloc) { score ->
                            ScoreDisplay(score = score)
                        }

                        // ── 3. BlocConsumer — tier badge rebuilds + animates only at tier boundaries ──
                        BlocConsumer(
                            bloc       = bloc,
                            listenWhen = { old, new -> Tier.of(old) != Tier.of(new) },
                            listener   = { _ ->
                                tierPulse = true
                            },
                            buildWhen  = { old, new -> Tier.of(old) != Tier.of(new) },
                        ) { score ->
                            TierBadge(tier = Tier.of(score), isPulsing = tierPulse)
                        }

                        ActionButtons(bloc = bloc)
                    }
                }
            }

            LaunchedEffect(milestoneText) {
                if (milestoneText != null) {
                    delay(2500)
                    milestoneText = null
                }
            }
            LaunchedEffect(tierPulse) {
                if (tierPulse) {
                    delay(400)
                    tierPulse = false
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeatureBadgeRow() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FeaturePill(icon = "🔔", text = "BlocListener — milestone side-effect", color = Color(0xFFFF9800))
        FeaturePill(icon = "⚡", text = "BlocConsumer — tier rebuild + animation", color = Color(0xFF00C8D8))
    }
}

@Composable
private fun FeaturePill(icon: String, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(color.copy(0.10f), RoundedCornerShape(50))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(icon, fontSize = 13.sp)
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(0.75f))
    }
}

@Composable
private fun ScoreDisplay(score: Int) {
    val tier = Tier.of(score)
    val borderColor by animateColorAsState(tier.color.copy(0.4f), tween(500), label = "border")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .background(Color.White.copy(0.04f), RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(vertical = 28.dp, horizontal = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "SCORE",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 5.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(0.45f),
        )
        AnimatedContent(
            targetState = score,
            transitionSpec = {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            },
            label = "score",
        ) { s ->
            Text(
                text  = "$s",
                fontSize = 96.sp,
                fontWeight = FontWeight.Thin,
                color = tier.gradientColors.first(),
            )
        }
        Text(
            "Next milestone at ${nextMilestone(score)} pts",
            fontSize = 13.sp,
            color = Color.White.copy(0.3f),
        )
    }
}

@Composable
private fun TierBadge(tier: Tier, isPulsing: Boolean) {
    val scale by animateFloatAsState(if (isPulsing) 1.06f else 1.0f, spring(), label = "tierScale")
    val bgAlpha by animateFloatAsState(if (isPulsing) 0.22f else 0.10f, tween(300), label = "tierBg")
    val borderAlpha by animateFloatAsState(if (isPulsing) 0.65f else 0.30f, tween(300), label = "tierBorder")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .scale(scale)
            .background(tier.color.copy(bgAlpha), RoundedCornerShape(14.dp))
            .border(if (isPulsing) 1.5.dp else 1.dp, tier.color.copy(borderAlpha), RoundedCornerShape(14.dp))
            .shadow(if (isPulsing) 14.dp else 0.dp, RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(tier.icon, fontSize = 22.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tier.label, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = tier.color)
            Text(tier.subtitle, fontSize = 12.sp, color = Color.White.copy(0.5f))
        }
    }
}

@Composable
private fun ActionButtons(bloc: ScoreBloc) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Score! button
        var pressed by remember { mutableStateOf(false) }
        val scale   by animateFloatAsState(if (pressed) 0.94f else 1f, spring(0.5f), label = "btn")

        Box(
            modifier = Modifier
                .scale(scale)
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .shadow(if (pressed) 0.dp else 12.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF9B5EF5), Color(0xFF5B20CC))))
                .clickable(
                    onClick = { pressed = true; bloc.send(ScoreEvent.AddPoint) },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("＋", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Score!", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        LaunchedEffect(pressed) { if (pressed) { delay(100); pressed = false } }

        // Reset button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(0.07f))
                .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(50))
                .clickable(
                    onClick = { bloc.send(ScoreEvent.Reset) },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("↺", fontSize = 15.sp, color = Color.White.copy(0.5f))
                Text("Reset", fontSize = 15.sp, color = Color.White.copy(0.5f), fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            "Every 5 pts → BlocListener fires. Tier badge: BlocConsumer rebuilds + animates at 10, 20, 30 pts.",
            fontSize = 12.sp,
            color = Color.White.copy(0.28f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun MilestoneBanner(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .background(Color(0xFF2D2344), RoundedCornerShape(50))
            .border(1.dp, Color(0xFFFFC107).copy(0.45f), RoundedCornerShape(50))
            .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        Text("⭐", fontSize = 16.sp)
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
