package io.github.spearchucker667.veniceforge.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.spearchucker667.veniceforge.core.designsystem.VeniceForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VeniceForgeTheme(darkTheme = true) {
                VeniceForgeApp()
            }
        }
    }
}
