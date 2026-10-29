package ai.voicedoc.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun SoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.border(1.dp, VoiceDocColors.Line, CardDefaults.shape),
        shape = CardDefaults.shape,
        colors = CardDefaults.cardColors(containerColor = VoiceDocColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = content
    )
}

@Composable
internal fun AssistantOrb(size: Dp = 118.dp, listening: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "assistant-orb")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (listening) 1.08f else 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (listening) 850 else 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb-pulse"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Box(
            Modifier.size(size * 0.98f).scale(pulse)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFE5F8ED), Color(0xFFC6EBD7), Color(0xFF9ED8B8))))
                .border(1.dp, Color.White.copy(alpha = 0.75f), CircleShape)
        )
        Box(
            Modifier.size(size * 0.67f)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF5CC78E), VoiceDocColors.Forest)))
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✦", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
        }
    }
}
