package dev.bloc.sample.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import dev.bloc.sample.examples.calculator.CalculatorBloc
import dev.bloc.sample.examples.calculator.CalculatorLogScreen
import dev.bloc.sample.examples.calculator.CalculatorScreen
import dev.bloc.sample.examples.counter.CounterBloc
import dev.bloc.sample.examples.counter.CounterScreen
import dev.bloc.sample.examples.formulaone.FormulaOneBloc
import dev.bloc.sample.examples.formulaone.FormulaOneScreen
import dev.bloc.sample.examples.heartbeat.HeartbeatScreen
import dev.bloc.sample.examples.lorcana.LorcanaBloc
import dev.bloc.sample.examples.lorcana.LorcanaCardDetailScreen
import dev.bloc.sample.examples.lorcana.LorcanaScreen
import dev.bloc.sample.examples.lorcana.LorcanaSetCardPlaceholder
import dev.bloc.sample.examples.lorcana.LorcanaSetDetailScreen
import dev.bloc.sample.examples.lorcana.models.LorcanaCard
import dev.bloc.sample.examples.scoreboard.ScoreBloc
import dev.bloc.sample.examples.scoreboard.ScoreScreen
import dev.bloc.sample.examples.stopwatch.StopwatchCubit
import dev.bloc.sample.examples.stopwatch.StopwatchScreen
import dev.bloc.sample.ui.home.HomeScreen
import dev.bloc.sample.ui.home.WelcomeDetailPane

enum class BlocDestination(
    val title: String,
    val subtitle: String,
    val accentStart: Long,
    val accentEnd: Long,
) {
    COUNTER(
        title = "Counter",
        subtitle = "HydratedBloc · state persistence",
        accentStart = 0xFF00BCD4,
        accentEnd = 0xFF006064,
    ),
    STOPWATCH(
        title = "Stopwatch",
        subtitle = "Cubit · direct methods · async tick",
        accentStart = 0xFF4CAF50,
        accentEnd = 0xFF1B5E20,
    ),
    CALCULATOR(
        title = "Calculator",
        subtitle = "Lifecycle hooks · onEvent · onChange · onTransition",
        accentStart = 0xFFFF9800,
        accentEnd = 0xFFBF360C,
    ),
    HEARTBEAT(
        title = "Heartbeat",
        subtitle = "Scoped Bloc · close() on dismiss",
        accentStart = 0xFFE91E63,
        accentEnd = 0xFF880E4F,
    ),
    SCOREBOARD(
        title = "Score Board",
        subtitle = "BlocListener · BlocBuilder buildWhen · BlocConsumer",
        accentStart = 0xFF9C27B0,
        accentEnd = 0xFF4A148C,
    ),
    FORMULA_ONE(
        title = "Formula One",
        subtitle = "Async API · enum states · network",
        accentStart = 0xFFF44336,
        accentEnd = 0xFFB71C1C,
    ),
    LORCANA(
        title = "Lorcana",
        subtitle = "Search · debounce · infinite scroll · BlocSelector",
        accentStart = 0xFF673AB7,
        accentEnd = 0xFF311B92,
    ),
}

/**
 * Root navigation using [ListDetailPaneScaffold] — the Android equivalent of iOS
 * NavigationSplitView.
 *
 * - **Phone / compact width**: single pane; list navigates to detail, back returns to list.
 * - **Foldable (expanded) / tablet**: two panes side-by-side; list stays visible on the left,
 *   selected example fills the right.
 *
 * Each example destination is a route inside a [NavHost] hosted in the detail pane so that
 * multi-screen examples (e.g. Lorcana card → set detail) can push their own back stack without
 * affecting the outer list.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun BlocSampleNavigation() {
    val navigator = rememberListDetailPaneScaffoldNavigator<BlocDestination>()
    val scope = rememberCoroutineScope()

    // The selected destination drives both list highlighting and detail content.
    // We check both Detail and Extra so that when a phone user taps "View Log"
    // (navigating to the Extra pane), the calculator is still considered selected.
    val selectedDestination by remember {
        derivedStateOf {
            navigator.currentDestination
                ?.takeIf {
                    it.pane == ListDetailPaneScaffoldRole.Detail ||
                        it.pane == ListDetailPaneScaffoldRole.Extra
                }
                ?.contentKey
        }
    }

    // True when the list pane is hidden so the detail pane can show a back button.
    val listIsHidden by remember {
        derivedStateOf {
            navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
        }
    }

    // True on foldable / tablet where two horizontal panes are visible simultaneously.
    val isExpandedLayout by remember {
        derivedStateOf {
            navigator.scaffoldDirective.maxHorizontalPartitions >= 2
        }
    }

    // True on phone when the lifecycle log (Extra pane) is not yet visible.
    // Used to show a "View Log" icon-button in the calculator top bar.
    val logIsHidden by remember {
        derivedStateOf {
            selectedDestination == BlocDestination.CALCULATOR &&
                navigator.scaffoldValue[ListDetailPaneScaffoldRole.Extra] == PaneAdaptedValue.Hidden
        }
    }

    // True on phone when the Extra pane fills the whole screen (Detail is behind it).
    // Used to show a back arrow inside the lifecycle log screen.
    val logPaneIsAlone by remember {
        derivedStateOf {
            navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Hidden &&
                navigator.scaffoldValue[ListDetailPaneScaffoldRole.Extra] != PaneAdaptedValue.Hidden
        }
    }

    // Calculator bloc hoisted here so it can be shared between the Detail pane
    // (calculator pad) and the Extra pane (lifecycle log).
    // DisposableEffect recreates/closes it whenever the selected destination changes.
    var calculatorBlocState by remember { mutableStateOf<CalculatorBloc?>(null) }
    DisposableEffect(selectedDestination) {
        if (selectedDestination == BlocDestination.CALCULATOR) {
            val bloc = CalculatorBloc()
            calculatorBlocState = bloc
            onDispose {
                bloc.close()
                calculatorBlocState = null
            }
        } else {
            onDispose { }
        }
    }

    // Lorcana bloc hoisted here so search results survive when ExampleDetailHost
    // temporarily leaves composition (e.g. while the set-exploration split is open).
    var lorcanaBlocState by remember { mutableStateOf<LorcanaBloc?>(null) }
    DisposableEffect(selectedDestination) {
        if (selectedDestination == BlocDestination.LORCANA) {
            val bloc = LorcanaBloc()
            lorcanaBlocState = bloc
            onDispose {
                bloc.close()
                lorcanaBlocState = null
            }
        } else {
            onDispose { }
        }
    }

    // Lorcana set-exploration state hoisted so both the Detail pane (set grid) and
    // the Extra pane (card detail) can share it — same pattern as Calculator/log.
    var lorcanaSetName by remember { mutableStateOf<String?>(null) }
    var lorcanaSetCard by remember { mutableStateOf<LorcanaCard?>(null) }
    val lorcanaSetCardCache = remember { mutableStateMapOf<String, List<LorcanaCard>>() }

    // True whenever the set-exploration split is active.
    val lorcanaSetIsOpen by remember {
        derivedStateOf {
            isExpandedLayout && selectedDestination == BlocDestination.LORCANA && lorcanaSetName != null
        }
    }

    // Reset Lorcana set state when switching to a different example.
    LaunchedEffect(selectedDestination) {
        if (selectedDestination != BlocDestination.LORCANA) {
            lorcanaSetName = null
            lorcanaSetCard = null
        }
    }

    // Hardware / gesture back — priority order (last registered = highest priority):
    // 1. Navigator back (phones: detail → list)
    BackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
    }
    // 2. Close the Lorcana set view (when no card is selected in the Extra pane)
    BackHandler(enabled = lorcanaSetIsOpen && lorcanaSetCard == null) {
        lorcanaSetName = null
        if (navigator.canNavigateBack()) scope.launch { navigator.navigateBack() }
    }
    // 3. Clear the selected card first (highest priority)
    BackHandler(enabled = lorcanaSetIsOpen && lorcanaSetCard != null) {
        lorcanaSetCard = null
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                HomeScreen(
                    selectedDestination = selectedDestination,
                    onNavigate = { dest ->
                        scope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, dest)
                            // On foldable / tablet (2+ horizontal panes) the Calculator gets
                            // a dedicated Extra pane for its lifecycle log. Navigating there
                            // collapses the list menu and shows the pad + log side by side.
                            if (dest == BlocDestination.CALCULATOR &&
                                navigator.scaffoldDirective.maxHorizontalPartitions >= 2
                            ) {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Extra, dest)
                            }
                        }
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                when {
                    // Lorcana set-exploration: show set grid in this pane.
                    lorcanaSetIsOpen -> LorcanaSetDetailScreen(
                        setName        = lorcanaSetName!!,
                        showBackButton = true,
                        onBack         = {
                            lorcanaSetName = null
                            lorcanaSetCard = null
                            if (navigator.canNavigateBack()) scope.launch { navigator.navigateBack() }
                        },
                        cachedCards    = lorcanaSetCardCache[lorcanaSetName],
                        onCardsLoaded  = { lorcanaSetCardCache[lorcanaSetName!!] = it },
                        onCardClick    = { card -> lorcanaSetCard = card },
                    )
                    selectedDestination != null -> ExampleDetailHost(
                        destination      = selectedDestination!!,
                        showBackButton   = listIsHidden,
                        onBack           = { scope.launch { navigator.navigateBack() } },
                        calculatorBloc   = calculatorBlocState,
                        lorcanaBloc      = lorcanaBlocState,
                        isExpandedLayout = isExpandedLayout,
                        onNavigateToSet  = if (isExpandedLayout && selectedDestination == BlocDestination.LORCANA) {
                            { setName ->
                                lorcanaSetName = setName
                                lorcanaSetCard = null
                                scope.launch {
                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Extra, BlocDestination.LORCANA)
                                }
                            }
                        } else null,
                        onShowLog        = if (logIsHidden) {
                            { scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Extra, BlocDestination.CALCULATOR) } }
                        } else null,
                    )
                    else -> WelcomeDetailPane()
                }
            }
        },
        extraPane = {
            AnimatedPane {
                val calcBloc = calculatorBlocState
                when {
                    selectedDestination == BlocDestination.CALCULATOR && calcBloc != null ->
                        CalculatorLogScreen(
                            bloc           = calcBloc,
                            showBackButton = logPaneIsAlone,
                            onBack         = { scope.launch { navigator.navigateBack() } },
                        )
                    lorcanaSetIsOpen -> {
                        val card = lorcanaSetCard
                        if (card != null) {
                            LorcanaCardDetailScreen(
                                card = card,
                                onNavigateToSet = { setName ->
                                    if (setName != lorcanaSetName) {
                                        lorcanaSetName = setName
                                        lorcanaSetCard = null
                                    }
                                },
                            )
                        } else {
                            LorcanaSetCardPlaceholder()
                        }
                    }
                }
            }
        },
    )
}

/**
 * Inner NavHost that lives inside the detail pane. When examples need multi-screen
 * navigation (e.g. Lorcana) they push routes here without disturbing the outer scaffold.
 *
 * Each [destination] gets its own [key] block, which tears down and re-creates the
 * entire composition — including any [remember]ed blocs — whenever the user switches
 * examples. This keeps lifecycle clean on both phones and tablets.
 *
 * [calculatorBloc] is provided externally (hoisted to navigation level) so the same
 * instance can be observed by both the Detail pane (pad) and the Extra pane (log).
 */
@Composable
private fun ExampleDetailHost(
    destination: BlocDestination,
    showBackButton: Boolean,
    onBack: () -> Unit,
    calculatorBloc: CalculatorBloc? = null,
    lorcanaBloc: LorcanaBloc? = null,
    isExpandedLayout: Boolean = false,
    onNavigateToSet: ((String) -> Unit)? = null,
    onShowLog: (() -> Unit)? = null,
) {
    key(destination) {
        when (destination) {

            BlocDestination.COUNTER -> {
                val counterBloc = remember { CounterBloc() }
                DisposableEffect(counterBloc) { onDispose { counterBloc.close() } }
                CounterScreen(
                    bloc           = counterBloc,
                    showBackButton = showBackButton,
                    onBack         = onBack,
                )
            }

            BlocDestination.STOPWATCH -> {
                val stopwatchCubit = remember { StopwatchCubit() }
                DisposableEffect(stopwatchCubit) { onDispose { stopwatchCubit.close() } }
                StopwatchScreen(
                    bloc           = stopwatchCubit,
                    showBackButton = showBackButton,
                    onBack         = onBack,
                )
            }

            BlocDestination.CALCULATOR -> {
                // Bloc lifecycle is managed by BlocSampleNavigation so it can be shared
                // with the Extra pane (lifecycle log). Guard against the brief null window
                // while the DisposableEffect creates the new instance.
                if (calculatorBloc != null) {
                    CalculatorScreen(
                        bloc           = calculatorBloc,
                        showBackButton = showBackButton,
                        onBack         = onBack,
                        onShowLog      = onShowLog,
                    )
                }
            }

            // HeartbeatScreen manages its own scoped Bloc internally.
            BlocDestination.HEARTBEAT -> {
                HeartbeatScreen(
                    showBackButton = showBackButton,
                    onBack         = onBack,
                )
            }

            BlocDestination.SCOREBOARD -> {
                val scoreBloc = remember { ScoreBloc() }
                DisposableEffect(scoreBloc) { onDispose { scoreBloc.close() } }
                ScoreScreen(
                    bloc           = scoreBloc,
                    showBackButton = showBackButton,
                    onBack         = onBack,
                )
            }

            BlocDestination.FORMULA_ONE -> {
                val f1Bloc = remember { FormulaOneBloc() }
                DisposableEffect(f1Bloc) { onDispose { f1Bloc.close() } }
                FormulaOneScreen(
                    bloc           = f1Bloc,
                    showBackButton = showBackButton,
                    onBack         = onBack,
                )
            }

            BlocDestination.LORCANA -> {
                // Guard against the brief null window while the DisposableEffect
                // at the navigation level creates the new instance.
                if (lorcanaBloc != null) {
                    LorcanaScreen(
                        bloc            = lorcanaBloc,
                        showBackButton  = showBackButton,
                        onBack          = onBack,
                        onNavigateToSet = onNavigateToSet,
                    )
                }
            }
        }
    }
}

