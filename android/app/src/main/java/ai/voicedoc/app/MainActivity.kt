package ai.voicedoc.app

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import ai.voicedoc.app.ui.VoiceDoctorApp
import ai.voicedoc.app.ui.VoiceDocTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(247, 250, 248)
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContent {
            VoiceDocTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VoiceDoctorApp()
                }
            }
        }
    }
}
