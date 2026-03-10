package dev.bloc.sample.examples.suvs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloc.sample.examples.suvs.models.AutoStopColor
import dev.bloc.sample.examples.suvs.models.SuvInstance
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Colours
// ─────────────────────────────────────────────────────────────────────────────

private val SuvDark    = Color(0xFF080F18)
private val SuvNavy    = Color(0xFF0D1826)
private val SuvSurface = Color(0xFF0F2030)
private val SuvTeal    = Color(0xFF00BCD4)
private val SuvBorder  = Color(0xFF1A3040)

// ─────────────────────────────────────────────────────────────────────────────
// Root
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SUVScreen(
    bloc: SUVBloc,
    showBackButton: Boolean,
    onBack: () -> Unit,
) {
    val state by bloc.stateFlow.collectAsState()

    var showSuccessBanner by remember { mutableStateOf(false) }
    var successInstanceName by remember { mutableStateOf("") }

    // Detect successful extend completion
    LaunchedEffect(state) {
        val loaded = state as? SUVState.Loaded ?: return@LaunchedEffect
        if (loaded.extendingId == null && successInstanceName.isNotEmpty()) {
            showSuccessBanner = true
            delay(3_000)
            showSuccessBanner = false
            successInstanceName = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SUV Instances", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SuvDark),
            )
        },
        containerColor = SuvDark,
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = state.isAuthenticated,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "authState",
            ) { isAuthenticated ->
                if (isAuthenticated) {
                    InstancesView(
                        state = state,
                        bloc  = bloc,
                        onExtendStart = { name -> successInstanceName = name },
                    )
                } else {
                    LoginView(state = state, bloc = bloc)
                }
            }

            // Success banner
            AnimatedVisibility(
                visible  = showSuccessBanner,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                enter    = slideInVertically { -it } + fadeIn(),
                exit     = slideOutVertically { -it } + fadeOut(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .background(Color(0xFF1A5C30), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text("✓", fontSize = 16.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                    Text(
                        "Extended $successInstanceName by 2 hours",
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Login view
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoginView(state: SUVState, bloc: SUVBloc) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading = state is SUVState.Authenticating
    val error     = (state as? SUVState.AuthError)?.message

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = SuvTeal,
        unfocusedBorderColor = SuvBorder,
        focusedTextColor     = Color.White,
        unfocusedTextColor   = Color.White,
        cursorColor          = SuvTeal,
    )

    fun performLogin() {
        if (!isLoading) bloc.send(SUVEvent.Login(username, password))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(SuvDark, SuvNavy))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(Brush.linearGradient(listOf(SuvTeal, Color(0xFF00ACC1))), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("🖥", fontSize = 36.sp) }
                Text("SUVify", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Manage your Single-User Versions", fontSize = 14.sp, color = Color.White.copy(0.65f))
            }

            // Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.04f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Username", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(0.75f))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Enter your username", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Next,
                        ),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Password", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(0.75f))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Enter your password", color = Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { performLogin() }),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            // Error
            if (error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Red.copy(0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⚠", fontSize = 16.sp, color = Color.Red)
                    Text(error, fontSize = 13.sp, color = Color.Red.copy(0.85f))
                }
            }

            // Sign-in button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isLoading)
                            Brush.linearGradient(listOf(SuvTeal.copy(0.5f), Color(0xFF00ACC1).copy(0.5f)))
                        else
                            Brush.linearGradient(listOf(SuvTeal, Color(0xFF00ACC1))),
                    )
                    .clickable(
                        enabled = !isLoading,
                        onClick = ::performLogin,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    Text("Signing in…", color = Color.White.copy(0.7f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("→", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Sign In", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text(
                "Use your Active Directory credentials to sign in.",
                fontSize = 11.sp, color = Color.White.copy(0.3f), fontFamily = FontFamily.Monospace,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Instances view
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InstancesView(
    state: SUVState,
    bloc: SUVBloc,
    onExtendStart: (String) -> Unit,
) {
    val user      = state.currentUser ?: return
    val instances = when (state) {
        is SUVState.Loaded           -> state.instances
        is SUVState.LoadingInstances -> emptyList()
        else                         -> emptyList()
    }
    val isLoading    = state is SUVState.LoadingInstances
    val extendingId  = (state as? SUVState.Loaded)?.extendingId
    val errorMessage = (state as? SUVState.Error)?.message

    Column(modifier = Modifier.fillMaxSize().background(SuvDark)) {
        // User header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(0.03f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Welcome back", fontSize = 11.sp, color = Color.Gray)
                Text(user.userName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            IconButton(onClick = { bloc.send(SUVEvent.Logout) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.Red.copy(0.75f))
            }
        }
        HorizontalDivider(color = SuvBorder)

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("⏳", fontSize = 36.sp)
                        Text("Loading your SUV instances...", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
            errorMessage != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("⚠️", fontSize = 40.sp)
                        Text("Something went wrong", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(errorMessage, color = Color.Gray, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SuvTeal.copy(0.15f))
                                .border(1.dp, SuvTeal.copy(0.4f), RoundedCornerShape(50))
                                .clickable(onClick = { bloc.send(SUVEvent.RefreshInstances) }, indication = null, interactionSource = remember { MutableInteractionSource() })
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Refresh, null, tint = SuvTeal, modifier = Modifier.size(16.dp))
                                Text("Try Again", color = SuvTeal, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            instances.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🖥", fontSize = 48.sp)
                        Text("No SUV Instances", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("You don't have any assigned instances.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(instances, key = { it.instanceId }) { instance ->
                        InstanceCard(
                            instance    = instance,
                            isExtending = instance.instanceId == extendingId,
                            onExtend    = {
                                onExtendStart(instance.wdDescription)
                                bloc.send(SUVEvent.ExtendInstance(instance.instanceId))
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Instance card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InstanceCard(
    instance: SuvInstance,
    isExtending: Boolean,
    onExtend: () -> Unit,
) {
    val stateColor = if (instance.state.isActive) Color(0xFF4CAF50) else Color(0xFFFF9800)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SuvSurface, RoundedCornerShape(16.dp))
            .border(1.dp, SuvBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    instance.instanceId,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(instance.wdDescription, fontSize = 12.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
            }
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(stateColor.copy(0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Box(Modifier.size(7.dp).background(stateColor, CircleShape))
                Text(instance.state.displayName, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = stateColor)
            }
        }

        // Details
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DetailRow(icon = "🌐", label = "Hostname", value = instance.wdHostname)
            instance.formattedAutoStop?.let { autoStop ->
                val stopColor = when (instance.autoStopColor) {
                    AutoStopColor.OK       -> Color(0xFF4CAF50)
                    AutoStopColor.WARNING  -> Color(0xFFFF9800)
                    AutoStopColor.CRITICAL -> Color.Red
                    AutoStopColor.EXPIRED  -> Color.Red.copy(0.6f)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⏱", fontSize = 12.sp)
                    Text("Auto-stop", fontSize = 11.sp, color = Color.Gray)
                    Text(autoStop, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = stopColor)
                }
            }
        }

        // Extend button
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isExtending) SuvTeal.copy(0.06f) else SuvTeal.copy(0.10f))
                    .border(1.dp, SuvTeal.copy(if (isExtending) 0.2f else 0.4f), RoundedCornerShape(10.dp))
                    .clickable(
                        enabled = !isExtending,
                        onClick = onExtend,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (isExtending) "⏳" else "🕐", fontSize = 13.sp)
                    Text(
                        if (isExtending) "Extending…" else "Extend 2h",
                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = if (isExtending) SuvTeal.copy(0.4f) else SuvTeal,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: String, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 12.sp)
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
