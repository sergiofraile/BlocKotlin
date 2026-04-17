package dev.bloc.sample.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bloc.sample.navigation.BlocDestination
import dev.bloc.sample.ui.theme.BlocBorder
import dev.bloc.sample.ui.theme.BlocNavy
import dev.bloc.sample.ui.theme.BlocNavyLight
import dev.bloc.sample.ui.theme.BlocSurface
import dev.bloc.sample.ui.theme.BlocSurfaceVar
import dev.bloc.sample.ui.theme.BlocTextPrimary
import dev.bloc.sample.ui.theme.BlocTextSecondary
import dev.bloc.sample.ui.theme.BlocTextTertiary

// ---------------------------------------------------------------------------
// Home / list screen
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(
    selectedDestination: BlocDestination?,
    onNavigate: (BlocDestination) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(BlocNavy, BlocNavyLight)),
            )
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
                start = 20.dp,
                end = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { HomeHeader() }
            item { Spacer(Modifier.height(4.dp)) }
            items(BlocDestination.entries) { destination ->
                ExampleCard(
                    destination = destination,
                    isSelected = destination == selectedDestination,
                    onClick = { onNavigate(destination) },
                )
            }
            item { HomeFooter() }
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Bloc",
            fontSize = 60.sp,
            fontWeight = FontWeight.Black,
            color = BlocTextPrimary,
            letterSpacing = (-2).sp,
        )
        Text(
            text = "Business Logic Component",
            fontSize = 16.sp,
            color = BlocTextSecondary,
            letterSpacing = 0.3.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Android · Kotlin · Jetpack Compose",
            fontSize = 12.sp,
            color = BlocTextTertiary,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = BlocBorder, thickness = 1.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${BlocDestination.entries.size} examples · tap to explore",
            fontSize = 12.sp,
            color = BlocTextTertiary,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ExampleCard(
    destination: BlocDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accentColors = listOf(
        Color(destination.accentStart),
        Color(destination.accentEnd),
    )

    val cardBackground = if (isSelected) BlocSurfaceVar else BlocSurface
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(accentColors),
            shape = RoundedCornerShape(14.dp),
        )
    } else {
        Modifier
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gradient initial circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(accentColors)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = destination.title.first().uppercaseChar().toString(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destination.title,
                    color = BlocTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = destination.subtitle,
                    color = BlocTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }

            Spacer(Modifier.width(8.dp))

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(accentColors)),
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = BlocTextTertiary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(color = BlocBorder, thickness = 1.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Inspired by bloclibrary.dev",
            fontSize = 12.sp,
            color = BlocTextTertiary,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ---------------------------------------------------------------------------
// Welcome pane — shown in the detail pane when no example is selected (tablet)
// ---------------------------------------------------------------------------

@Composable
fun WelcomeDetailPane() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlocNavy),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Bloc",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = BlocTextPrimary.copy(alpha = 0.08f),
                letterSpacing = (-4).sp,
            )
            Text(
                text = "Select an example",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = BlocTextSecondary,
            )
            Text(
                text = "from the list on the left",
                fontSize = 14.sp,
                color = BlocTextTertiary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Placeholder detail screen (replaced per-example in later TODOs)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    destination: BlocDestination,
    showBackButton: Boolean,
    onBack: () -> Unit,
) {
    val accentColors = listOf(
        Color(destination.accentStart),
        Color(destination.accentEnd),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        destination.title,
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
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(accentColors)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = destination.title.first().uppercaseChar().toString(),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = destination.title,
                    color = BlocTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Coming soon",
                    color = BlocTextTertiary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
