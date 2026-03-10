package dev.bloc.sample.examples.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Local palette
// ─────────────────────────────────────────────────────────────────────────────

private val CalcBackground  = Color(0xFF0D0D14)
private val CalcSurface     = Color(0xFF2E2E38)
private val CalcFunction    = Color(0xFF47474F)
private val CalcDigitFill   = Color(0xFF1C1C24)
private val CalcOpFill      = Color(0xFFBF6900)
private val CalcOpActive    = Color(0xFFBF6900).copy(alpha = 0.25f)
private val CalcEquals      = Color(0xFF2DB66A)
private val OrangeAccent    = Color(0xFFFF9500)
private val LogSurface      = Color(0xFF0A0A12)

// ─────────────────────────────────────────────────────────────────────────────
// Screen root — adaptive two-pane layout
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    bloc: CalculatorBloc,
    showBackButton: Boolean,
    onBack: () -> Unit,
    // Non-null on phone when the lifecycle log pane is not visible — shows a top-bar button.
    onShowLog: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Calculator — Lifecycle Hooks",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                    }
                },
                actions = {
                    if (onShowLog != null) {
                        IconButton(onClick = onShowLog) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "View Lifecycle Log",
                                tint = OrangeAccent,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CalcBackground),
            )
        },
        containerColor = CalcBackground,
    ) { padding ->
        CalculatorPad(
            bloc     = bloc,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lifecycle log screen — lives in the adaptive Extra pane
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wraps [LifecycleLogPanel] for display in the adaptive Extra pane.
 *
 * On phone the Extra pane fills the whole screen, so [showBackButton] is true and
 * the user can navigate back to the calculator pad. On foldable/tablet it is shown
 * alongside the calculator, so no back button is needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorLogScreen(
    bloc: CalculatorBloc,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
) {
    if (showBackButton) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Calculator",
                                tint = Color.White,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LogSurface),
                )
            },
            containerColor = LogSurface,
        ) { padding ->
            LifecycleLogPanel(
                bloc     = bloc,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    } else {
        LifecycleLogPanel(
            bloc     = bloc,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Calculator pad
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CalculatorPad(
    bloc: CalculatorBloc,
    modifier: Modifier = Modifier,
) {
    val state    by bloc.stateFlow.collectAsState()
    val isClosed by bloc.closedFlow.collectAsState()

    Box(
        modifier = modifier
            .background(CalcBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .scale(if (isClosed) 0.97f else 1f),
        ) {
            // Display
            CalcDisplay(state = state, isClosed = isClosed)

            Spacer(Modifier.height(12.dp))

            // Button grid — weight(1f) claims all remaining height so BoxWithConstraints
            // receives both maxWidth and maxHeight, letting us pick the largest square
            // button size that fits in both dimensions.
            val spacing = 9.dp
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val maxBtnFromWidth  = ((maxWidth  - spacing * 3) / 4).coerceAtMost(112.dp)
                val maxBtnFromHeight = ((maxHeight - spacing * 4) / 5).coerceAtMost(112.dp)
                val btnSize  = minOf(maxBtnFromWidth, maxBtnFromHeight)
                val gridWidth = btnSize * 4 + spacing * 3
                val sidePad  = ((maxWidth - gridWidth) / 2).coerceAtLeast(0.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sidePad),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    // Row 1: AC +/− % ÷
                    CalcRow(spacing) {
                        CalcBtn("AC",  ButtonKind.FUNCTION, btnSize) { bloc.send(CalculatorEvent.Clear) }
                        CalcBtn("+/−", ButtonKind.FUNCTION, btnSize) { bloc.send(CalculatorEvent.ToggleSign) }
                        CalcBtn("%",   ButtonKind.FUNCTION, btnSize) { bloc.send(CalculatorEvent.Percentage) }
                        CalcBtn("÷",   ButtonKind.OPERATION, btnSize,
                            isActive = state.pendingOperation == Operation.Divide,
                        ) { bloc.send(CalculatorEvent.Op(Operation.Divide)) }
                    }
                    // Row 2: 7 8 9 ×
                    CalcRow(spacing) {
                        CalcBtn("7", ButtonKind.DIGIT, btnSize) { bloc.send(CalculatorEvent.Digit(7)) }
                        CalcBtn("8", ButtonKind.DIGIT, btnSize) { bloc.send(CalculatorEvent.Digit(8)) }
                        CalcBtn("9", ButtonKind.DIGIT, btnSize) { bloc.send(CalculatorEvent.Digit(9)) }
                        CalcBtn("×", ButtonKind.OPERATION, btnSize,
                            isActive = state.pendingOperation == Operation.Multiply,
                        ) { bloc.send(CalculatorEvent.Op(Operation.Multiply)) }
                    }
                    // Row 3: 4 5 6 −
                    CalcRow(spacing) {
                        CalcBtn("4", ButtonKind.DIGIT, btnSize) { bloc.send(CalculatorEvent.Digit(4)) }
                        CalcBtn("5", ButtonKind.DIGIT, btnSize) { bloc.send(CalculatorEvent.Digit(5)) }
                        CalcBtn("6", ButtonKind.DIGIT, btnSize) { bloc.send(CalculatorEvent.Digit(6)) }
                        CalcBtn("−", ButtonKind.OPERATION, btnSize,
                            isActive = state.pendingOperation == Operation.Subtract,
                        ) { bloc.send(CalculatorEvent.Op(Operation.Subtract)) }
                    }
                    // Row 4: 1 2 3 +
                    CalcRow(spacing) {
                        CalcBtn("1", ButtonKind.DIGIT, btnSize) { bloc.send(CalculatorEvent.Digit(1)) }
                        CalcBtn("2", ButtonKind.DIGIT, btnSize) { bloc.send(CalculatorEvent.Digit(2)) }
                        CalcBtn("3", ButtonKind.DIGIT, btnSize) { bloc.send(CalculatorEvent.Digit(3)) }
                        CalcBtn("+", ButtonKind.OPERATION, btnSize,
                            isActive = state.pendingOperation == Operation.Add,
                        ) { bloc.send(CalculatorEvent.Op(Operation.Add)) }
                    }
                    // Row 5: 0 . ⌫ = (uniform size — avoids weight cross-axis issues)
                    CalcRow(spacing) {
                        CalcBtn("0",  ButtonKind.DIGIT,    btnSize) { bloc.send(CalculatorEvent.Digit(0)) }
                        CalcBtn(".",  ButtonKind.DIGIT,    btnSize) { bloc.send(CalculatorEvent.Decimal) }
                        CalcBtn("⌫", ButtonKind.FUNCTION, btnSize) { bloc.send(CalculatorEvent.Delete) }
                        CalcBtn("=",  ButtonKind.EQUALS,   btnSize) { bloc.send(CalculatorEvent.Equals) }
                    }
                }
            }
        }

        // Closed overlay
        AnimatedVisibility(
            visible = isClosed,
            enter   = scaleIn(initialScale = 0.85f) + fadeIn(),
            exit    = scaleOut() + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CalcBackground.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .background(CalcSurface, RoundedCornerShape(24.dp))
                        .padding(horizontal = 32.dp, vertical = 28.dp),
                ) {
                    Text("✕", fontSize = 36.sp, color = OrangeAccent)
                    Text(
                        "Bloc Closed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        "send() and emit() are no-ops.\nIn a real app, navigate away or\nreplace the Bloc to continue.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcDisplay(state: CalculatorState, isClosed: Boolean) {
    val fontSize = when {
        state.displayValue.length < 7  -> 64.sp
        state.displayValue.length < 10 -> 48.sp
        else                           -> 36.sp
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            // Pending operation indicator
            Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.CenterEnd) {
                if (state.pendingOperation != null) {
                    Text(
                        text = state.pendingOperation.symbol,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        color = OrangeAccent.copy(alpha = 0.8f),
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
            Text(
                text = state.displayValue,
                style = TextStyle(
                    brush = if (state.hasError)
                        Brush.horizontalGradient(listOf(Color(0xFFF44336), Color(0xFFFF9800)))
                    else
                        Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.88f))),
                    fontSize   = fontSize,
                    fontWeight = FontWeight.Thin,
                    fontFamily = FontFamily.SansSerif,
                    textAlign  = TextAlign.End,
                ),
                maxLines  = 1,
                overflow  = TextOverflow.Clip,
                modifier  = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Calculator button
// ─────────────────────────────────────────────────────────────────────────────

private enum class ButtonKind { DIGIT, FUNCTION, OPERATION, EQUALS }

@Composable
private fun CalcRow(spacing: Dp, content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun CalcBtn(
    label:    String,
    kind:     ButtonKind,
    size:     Dp,
    isActive: Boolean  = false,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label         = "calc_btn_scale",
    )

    val fillColor = when (kind) {
        ButtonKind.DIGIT     -> CalcDigitFill
        ButtonKind.FUNCTION  -> CalcFunction
        ButtonKind.OPERATION -> if (isActive) CalcOpActive else CalcOpFill
        ButtonKind.EQUALS    -> CalcEquals
    }
    val borderColor = when (kind) {
        ButtonKind.OPERATION -> OrangeAccent.copy(alpha = if (isActive) 0.55f else 0.18f)
        else                 -> Color.White.copy(alpha = 0.06f)
    }
    val textColor = when (kind) {
        ButtonKind.FUNCTION -> Color(0xFFE0E0E0)
        else                -> Color.White
    }
    val fontWeight = if (kind == ButtonKind.DIGIT) FontWeight.Normal else FontWeight.SemiBold

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(fillColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            color      = textColor,
            fontSize   = 22.sp,
            fontWeight = fontWeight,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lifecycle log panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LifecycleLogPanel(
    bloc: CalculatorBloc,
    modifier: Modifier = Modifier,
) {
    val entries  by bloc.logFlow.collectAsState()
    val isClosed by bloc.closedFlow.collectAsState()
    var showCloseDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new entry arrives
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    Column(
        modifier = modifier.background(LogSurface),
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.025f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Row 1: title + status badge + action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        "Lifecycle Log",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    // Status badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(
                                (if (isClosed) OrangeAccent else Color(0xFF4CAF50)).copy(alpha = 0.12f),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    if (isClosed) OrangeAccent else Color(0xFF4CAF50),
                                    CircleShape,
                                ),
                        )
                        Text(
                            text = if (isClosed) "CLOSED" else "ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isClosed) OrangeAccent else Color(0xFF4CAF50),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Text(
                        "${entries.size} events",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.35f),
                    )
                }

                // Close bloc button
                IconButton(
                    onClick  = { showCloseDialog = true },
                    enabled  = !isClosed,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Close Bloc",
                        tint = if (isClosed) Color.White.copy(0.2f) else OrangeAccent.copy(0.75f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                // Clear log button
                IconButton(
                    onClick  = { bloc.clearLog() },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear log",
                        tint = Color.White.copy(0.3f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Row 2: Legend pills
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LogEntry.Kind.entries.forEach { kind ->
                    KindPill(kind)
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

        // Entries
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("〜", fontSize = 30.sp, color = Color.White.copy(alpha = 0.15f))
                    Text(
                        "No events yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.3f),
                    )
                    Text(
                        "Tap a calculator button\nto watch the hooks fire.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.2f),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(entries, key = { it.id.toString() }) { entry ->
                    LogRow(entry = entry)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                }
            }
        }
    }

    // Close confirmation dialog
    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title   = { Text("Close Bloc?") },
            text    = {
                Text(
                    "Simulates navigating away from a screen with a scoped Bloc. " +
                        "send() and emit() become no-ops, flows complete, and onClose fires.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloseDialog = false
                        bloc.close()
                    },
                ) {
                    Text("Close Bloc", color = OrangeAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = CalcSurface,
            titleContentColor = Color.White,
            textContentColor  = Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun KindPill(kind: LogEntry.Kind) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .background(kind.color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(kind.icon, fontSize = 7.sp, color = kind.color)
        Text(
            text = kind.label,
            fontSize = 7.sp,
            fontWeight = FontWeight.SemiBold,
            color = kind.color,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val rowBackground = when (entry.kind) {
        LogEntry.Kind.ERROR -> Color.Red.copy(alpha = 0.06f)
        LogEntry.Kind.CLOSE -> OrangeAccent.copy(alpha = 0.06f)
        else                -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Kind badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .width(86.dp)
                .background(entry.kind.color.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
                .padding(horizontal = 5.dp, vertical = 3.dp),
        ) {
            Text(entry.kind.icon, fontSize = 9.sp, color = entry.kind.color)
            Text(
                text = entry.kind.label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = entry.kind.color,
                fontFamily = FontFamily.Monospace,
            )
        }

        // Message
        Text(
            text     = entry.message,
            fontSize = 11.sp,
            color    = Color.White.copy(alpha = 0.78f),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )

        // Timestamp
        Text(
            text   = entry.timestampFormatted,
            fontSize = 9.sp,
            color  = Color.White.copy(alpha = 0.25f),
            fontFamily = FontFamily.Monospace,
        )
    }
}
