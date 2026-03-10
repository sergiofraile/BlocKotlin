package dev.bloc.sample.examples.stopwatch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Local palette — mint green theme
// ─────────────────────────────────────────────────────────────────────────────

private val MintGreen     = Color(0xFF1ADB8A)
private val MintDark      = Color(0xFF0D9960)
private val MintFaint     = Color(0xFF0D6B44)
private val AmberStart    = Color(0xFFFFB300)
private val AmberEnd      = Color(0xFFE65100)
private val BackgroundTop = Color(0xFF050E0A)
private val BackgroundBot = Color(0xFF060F0C)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen(
    bloc: StopwatchCubit,
    showBackButton: Boolean,
    onBack: () -> Unit,
) {
    val state by bloc.stateFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Stopwatch",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundTop),
            )
        },
        containerColor = BackgroundTop,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(listOf(BackgroundTop, BackgroundBot))
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Pulsing radial glow — always composited, controlled by isRunning
            PulsingGlow(isRunning = state.isRunning)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                CubitBadge()

                Spacer(Modifier.height(36.dp))

                ClockFace(state = state)

                Spacer(Modifier.height(44.dp))

                Controls(
                    isRunning = state.isRunning,
                    canReset  = state.elapsed > 0.0 || state.isRunning,
                    onStart   = bloc::start,
                    onPause   = bloc::pause,
                    onReset   = bloc::reset,
                )

                Spacer(Modifier.height(36.dp))

                MethodChips()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pulsing radial glow background
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PulsingGlow(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue  = 0.18f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1_500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_pulse",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isRunning) pulse else 0f,
        animationSpec = tween(800),
        label = "glow_alpha",
    )
    Box(
        modifier = Modifier
            .size(600.dp)
            .background(
                Brush.radialGradient(
                    listOf(
                        MintGreen.copy(alpha = alpha),
                        Color.Transparent,
                    )
                ),
                CircleShape,
            ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Cubit badge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CubitBadge() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MintGreen.copy(alpha = 0.08f), RoundedCornerShape(50))
            .border(1.dp, MintGreen.copy(alpha = 0.22f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text("⚡", fontSize = 12.sp)
        Text(
            text = "Cubit — no events, direct method calls",
            fontSize = 11.sp,
            color = MintGreen.copy(alpha = 0.85f),
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Clock face — outer ring + inner glass circle + digits
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClockFace(state: StopwatchState) {
    // Rotating arc ring driven by an Animatable
    val ringAngle = remember { Animatable(0f) }
    LaunchedEffect(state.isRunning) {
        if (state.isRunning) {
            while (true) {
                ringAngle.animateTo(
                    targetValue   = ringAngle.value + 360f,
                    animationSpec = tween(durationMillis = 4_000, easing = LinearEasing),
                )
            }
        }
        // When isRunning goes false the effect is cancelled; angle stays in place
    }

    // Blinking colon
    val infiniteTransition = rememberInfiniteTransition(label = "colon")
    val colonAlpha by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.35f,
        animationSpec = infiniteRepeatable(
            animation  = tween(500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "colon",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(300.dp),
    ) {
        // Outer rotating arc ring (Canvas)
        Canvas(modifier = Modifier.size(300.dp)) {
            val strokeWidth = 2.5.dp.toPx()
            if (state.isRunning) {
                rotate(degrees = ringAngle.value, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colorStops = arrayOf(
                                0.00f to MintGreen.copy(alpha = 0f),
                                0.55f to MintDark,
                                0.75f to MintGreen,
                                1.00f to MintGreen.copy(alpha = 0f),
                            ),
                            center = center,
                        ),
                        startAngle  = 0f,
                        sweepAngle  = 360f,
                        useCenter   = false,
                        style       = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            } else {
                drawCircle(
                    color  = Color.White.copy(alpha = 0.07f),
                    radius = size.minDimension / 2f - strokeWidth / 2f,
                    style  = Stroke(strokeWidth),
                )
            }
        }

        // Inner frosted glass circle
        Box(
            modifier = Modifier
                .size(278.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    ),
                    CircleShape,
                )
                .border(1.dp, Color.White.copy(alpha = 0.09f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // MM:SS (animated on each full-second change)
                AnimatedContent(
                    targetState = "${state.minutesDisplay}:${state.secondsDisplay}",
                    transitionSpec = {
                        slideInVertically { -it / 4 } + fadeIn() togetherWith
                            slideOutVertically { it / 4 } + fadeOut()
                    },
                    label = "mmss",
                ) { mmss ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        val (mm, ss) = mmss.split(":")
                        Text(
                            text  = mm,
                            style = TextStyle(
                                brush       = Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.85f))),
                                fontSize    = 68.sp,
                                fontWeight  = FontWeight.Thin,
                                fontFamily  = FontFamily.Monospace,
                            ),
                        )
                        Text(
                            text  = ":",
                            style = TextStyle(
                                color       = Color.White.copy(alpha = if (state.isRunning) colonAlpha else 0.5f),
                                fontSize    = 60.sp,
                                fontWeight  = FontWeight.Thin,
                                fontFamily  = FontFamily.Monospace,
                            ),
                        )
                        Text(
                            text  = ss,
                            style = TextStyle(
                                brush       = Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.85f))),
                                fontSize    = 68.sp,
                                fontWeight  = FontWeight.Thin,
                                fontFamily  = FontFamily.Monospace,
                            ),
                        )
                    }
                }

                // .CS centiseconds
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = ".",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text  = state.centisecondsDisplay,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Controls — reset (small circle) + start/pause (large circle)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Controls(
    isRunning: Boolean,
    canReset: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
) {
    val playPauseGradient by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = tween(350),
        label = "btn_color",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Reset button
        val resetAlpha by animateFloatAsState(
            targetValue   = if (canReset) 1f else 0.3f,
            animationSpec = tween(300),
            label         = "reset_alpha",
        )
        SmallCircleButton(
            alpha   = resetAlpha,
            enabled = canReset,
            onClick = onReset,
        ) {
            Icon(
                imageVector        = Icons.Default.Refresh,
                contentDescription = "Reset",
                tint               = Color.White.copy(alpha = 0.7f),
                modifier           = Modifier.size(24.dp),
            )
        }

        // Start / Pause
        val startGradient = lerp(
            Brush.linearGradient(listOf(MintGreen, MintDark)),
            Brush.linearGradient(listOf(AmberStart, AmberEnd)),
            playPauseGradient,
        )
        val glowColor = if (isRunning) AmberStart else MintGreen
        LargePlayPauseButton(
            isRunning = isRunning,
            glowColor = glowColor,
            onClick   = { if (isRunning) onPause() else onStart() },
        )

        // Invisible spacer to balance the row
        Box(Modifier.size(60.dp))
    }
}

/** Linear interpolate between two floats for colour blending in gradient. */
private fun lerp(a: Brush, b: Brush, t: Float): Brush {
    // For animation clarity, we switch at midpoint
    return if (t < 0.5f) a else b
}

@Composable
private fun SmallCircleButton(
    alpha: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "small_btn_scale",
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(60.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f * alpha))
            .border(1.dp, Color.White.copy(alpha = 0.13f * alpha), CircleShape)
            .clickable(
                enabled           = enabled,
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.scale(alpha)) { content() }
    }
}

@Composable
private fun LargePlayPauseButton(
    isRunning: Boolean,
    glowColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label         = "play_scale",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .shadow(elevation = 24.dp, shape = CircleShape, ambientColor = glowColor.copy(0.4f), spotColor = glowColor.copy(0.5f))
            .size(88.dp)
            .clip(CircleShape)
            .background(
                if (isRunning)
                    Brush.linearGradient(listOf(AmberStart, AmberEnd))
                else
                    Brush.linearGradient(listOf(MintGreen, MintDark))
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isRunning) "Pause" else "Start",
            tint               = Color.White,
            modifier           = Modifier.size(34.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Footer — method call chips
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MethodChips() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("cubit.start()", "cubit.pause()", "cubit.reset()").forEach { label ->
                Text(
                    text     = label,
                    fontSize = 10.sp,
                    color    = MintGreen.copy(alpha = 0.75f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(MintFaint.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, MintGreen.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Text(
            text     = "State managed by StopwatchCubit — no events, no transformers.",
            fontSize = 11.sp,
            color    = Color.White.copy(alpha = 0.3f),
            fontFamily = FontFamily.Monospace,
        )
    }
}
