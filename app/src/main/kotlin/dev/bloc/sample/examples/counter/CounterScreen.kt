package dev.bloc.sample.examples.counter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloc.compose.BlocBuilder
import dev.bloc.sample.ui.theme.BlocNavy
import dev.bloc.sample.ui.theme.BlocTextPrimary
import dev.bloc.sample.ui.theme.BlocTextSecondary
import dev.bloc.sample.ui.theme.BlocTextTertiary

// ─────────────────────────────────────────────────────────────────────────────
// Local palette
// ─────────────────────────────────────────────────────────────────────────────

private val CyanAccent  = Color(0xFF00BCD4)
private val CoralStart  = Color(0xFFEF5350)
private val CoralEnd    = Color(0xFFC62828)
private val TealStart   = Color(0xFF4DB6AC)
private val TealEnd     = Color(0xFF00796B)
private val PurpleGlow  = Color(0xFF9C27B0)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    bloc: CounterBloc,
    showBackButton: Boolean,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Counter",
                        color = BlocTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = BlocTextPrimary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlocNavy),
            )
        },
        containerColor = BlocNavy,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Decorative ambient glow circles — radialGradient fades naturally to
            // transparent so there are no hard rectangular edges like blur() produces.
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-80).dp, y = (-40).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(CyanAccent.copy(alpha = 0.18f), Color.Transparent),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = (-60).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(PurpleGlow.copy(alpha = 0.22f), Color.Transparent),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.height(32.dp))

                HydrationBadge()

                Spacer(Modifier.height(48.dp))

                BlocBuilder(bloc = bloc) { count ->
                    CounterCard(count = count)
                }

                Spacer(Modifier.height(52.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleActionButton(
                        gradient  = Brush.linearGradient(listOf(CoralStart, CoralEnd)),
                        glowColor = CoralStart,
                        label     = "−",
                        onClick   = { bloc.send(CounterEvent.Decrement) },
                    )
                    CircleActionButton(
                        gradient  = Brush.linearGradient(listOf(TealStart, TealEnd)),
                        glowColor = TealStart,
                        label     = "+",
                        onClick   = { bloc.send(CounterEvent.Increment) },
                    )
                }

                Spacer(Modifier.height(40.dp))

                ResetControls(bloc = bloc)

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HydrationBadge() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(CyanAccent.copy(alpha = 0.08f), RoundedCornerShape(50))
            .border(1.dp, CyanAccent.copy(alpha = 0.22f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text("💾", fontSize = 13.sp)
        Text(
            text = "HydratedBloc — state persists across launches",
            fontSize = 11.sp,
            color = CyanAccent.copy(alpha = 0.85f),
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun CounterCard(count: Int) {
    Box(
        modifier = Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                ),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(1.dp)
            .background(BlocNavy, RoundedCornerShape(27.dp))
            .padding(horizontal = 48.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "COUNTER",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 7.sp,
                color = CyanAccent.copy(alpha = 0.65f),
            )

            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    val goingUp = targetState > initialState
                    if (goingUp) {
                        slideInVertically { it / 3 } + fadeIn() togetherWith
                            slideOutVertically { -it / 3 } + fadeOut()
                    } else {
                        slideInVertically { -it / 3 } + fadeIn() togetherWith
                            slideOutVertically { it / 3 } + fadeOut()
                    }
                },
                label = "counter_number",
            ) { value ->
                Text(
                    text = "$value",
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            listOf(Color.White, CyanAccent.copy(alpha = 0.7f)),
                        ),
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Thin,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    gradient: Brush,
    glowColor: Color,
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.87f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh,
        ),
        label = "btn_scale",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .shadow(
                elevation   = 20.dp,
                shape       = CircleShape,
                ambientColor = glowColor.copy(alpha = 0.35f),
                spotColor   = glowColor.copy(alpha = 0.5f),
            )
            .size(74.dp)
            .clip(CircleShape)
            .background(gradient)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            color      = Color.White,
            fontSize   = 30.sp,
            fontWeight = FontWeight.Light,
        )
    }
}

@Composable
private fun ResetControls(bloc: CounterBloc) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth(),
    ) {
        // Section divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.07f),
            )
            Text(
                text = "RESET",
                fontSize = 9.sp,
                color = BlocTextTertiary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.07f),
            )
        }

        Spacer(Modifier.height(2.dp))

        PillButton(
            label           = "↺   Reset  (persists 0)",
            textColor       = BlocTextSecondary,
            backgroundColor = Color.White.copy(alpha = 0.05f),
            borderColor     = Color.White.copy(alpha = 0.11f),
            onClick         = { bloc.send(CounterEvent.Reset) },
        )

        PillButton(
            label           = "🗑   Clear Stored State + Reset",
            textColor       = CyanAccent.copy(alpha = 0.85f),
            backgroundColor = CyanAccent.copy(alpha = 0.07f),
            borderColor     = CyanAccent.copy(alpha = 0.28f),
            onClick         = { bloc.resetToInitialState() },
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text      = "Increment, quit the app, relaunch —\nthe count is restored from SharedPreferences.",
            fontSize  = 12.sp,
            color     = BlocTextTertiary,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun PillButton(
    label: String,
    textColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue    = if (isPressed) 0.65f else 1f,
        animationSpec  = spring(stiffness = Spring.StiffnessHigh),
        label          = "pill_alpha",
    )

    Box(
        modifier = Modifier
            .scale(if (isPressed) 0.97f else 1f)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            color      = textColor.copy(alpha = alpha),
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
