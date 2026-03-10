package dev.bloc.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.bloc.sample.navigation.BlocSampleNavigation
import dev.bloc.sample.ui.theme.BlocTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlocTheme {
                BlocSampleNavigation()
            }
        }
    }
}
