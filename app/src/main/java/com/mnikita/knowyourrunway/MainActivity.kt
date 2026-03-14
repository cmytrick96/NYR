package com.mnikita.knowyourrunway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mnikita.knowyourrunway.data.AccentPreset
import com.mnikita.knowyourrunway.data.ThemeMode
import com.mnikita.knowyourrunway.data.ThemeStore
import com.mnikita.knowyourrunway.ui.AppRoot
import com.mnikita.knowyourrunway.ui.theme.NYRTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ctx = LocalContext.current

            val themeMode by ThemeStore.themeModeFlow(ctx).collectAsState(initial = ThemeMode.SYSTEM)
            val accent by ThemeStore.accentFlow(ctx).collectAsState(initial = AccentPreset.COFFEE)

            NYRTheme(themeMode = themeMode, accentPreset = accent) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}