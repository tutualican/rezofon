package studio.rezofon.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import studio.rezofon.app.audio.ToneGenerator
import studio.rezofon.app.state.AppState
import studio.rezofon.app.ui.theme.LocalRzPalette
import studio.rezofon.app.ui.theme.RezofonTheme

enum class Dest(val label: String, val icon: ImageVector) {
    Home("Ana", Icons.Rounded.SpaceDashboard),
    Eq("EQ", Icons.Rounded.GraphicEq),
    Bass("Bas", Icons.Rounded.Tune),
    Device("Cihaz", Icons.Rounded.Bluetooth),
    Workshop("Atölye", Icons.Rounded.Science),
}

private sealed interface Overlay {
    data object None : Overlay
    data object Guard : Overlay
    data object Settings : Overlay
}

@Composable
fun RezofonRoot(state: AppState) {
    val tone = remember { ToneGenerator() }
    var splash by remember { mutableStateOf(true) }
    var dest by remember { mutableStateOf(Dest.Home) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var toast by remember { mutableStateOf<String?>(null) }
    val p = LocalRzPalette.current

    LaunchedEffect(Unit) {
        state.connectEngine()
        state.refreshDevice()
        while (true) {
            delay(4000)
            state.refreshDevice()
        }
    }

    LaunchedEffect(state.notice) {
        val msg = state.consumeNotice()
        if (msg != null) {
            toast = msg
            delay(2600)
            toast = null
        }
    }

    RezofonTheme(dark = state.darkMode) {
        RzBackdrop {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    AnimatedContent(
                        targetState = overlay,
                        transitionSpec = {
                            (fadeIn(tween(200)) + slideInHorizontally { it / 14 })
                                .togetherWith(fadeOut(tween(140)))
                        },
                        label = "overlay",
                    ) { ov ->
                        when (ov) {
                            Overlay.Guard -> GuardScreen(
                                state = state,
                                onBack = { overlay = Overlay.None },
                            )

                            Overlay.Settings -> SettingsScreen(
                                state = state,
                                onBack = { overlay = Overlay.None },
                            )

                            Overlay.None -> AnimatedContent(
                                targetState = dest,
                                transitionSpec = {
                                    (fadeIn(tween(220)) + slideInHorizontally { it / 10 })
                                        .togetherWith(fadeOut(tween(120)))
                                },
                                label = "dest",
                            ) { d ->
                                when (d) {
                                    Dest.Home -> HomeScreen(
                                        state = state,
                                        onOpenEq = { dest = Dest.Eq },
                                        onOpenBass = { dest = Dest.Bass },
                                        onOpenDevice = { dest = Dest.Device },
                                        onOpenWorkshop = { dest = Dest.Workshop },
                                        onOpenGuard = { overlay = Overlay.Guard },
                                        onOpenSettings = { overlay = Overlay.Settings },
                                        onApplyPreset = { state.applyPreset(it) },
                                    )

                                    Dest.Eq -> EqScreen(state)
                                    Dest.Bass -> BassScreen(state)
                                    Dest.Device -> DeviceScreen(state, onOpenGuard = { overlay = Overlay.Guard })
                                    Dest.Workshop -> WorkshopScreen(
                                        state = state,
                                        tone = tone,
                                        onOpenGuard = { overlay = Overlay.Guard },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (overlay == Overlay.None) {
                    Dock(
                        current = dest,
                        onSelect = { dest = it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                    )
                }

                AnimatedVisibility(
                    visible = toast != null,
                    enter = fadeIn(tween(160)),
                    exit = fadeOut(tween(160)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp),
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(p.text)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            toast ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = p.bg,
                        )
                    }
                }

                if (splash) {
                    SplashOverlay { splash = false }
                }
            }
        }
    }
}

@Composable
private fun Dock(
    current: Dest,
    onSelect: (Dest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalRzPalette.current
    Row(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(p.surface.copy(alpha = if (p.isDark) 0.92f else 0.96f))
            .border(1.dp, p.outline, RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dest.entries.forEach { d ->
            val selected = d == current
            Column(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) p.lime else Color.Transparent)
                    .clickable { onSelect(d) }
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    d.icon,
                    d.label,
                    tint = if (selected) Color(0xFF14160F) else p.textDim,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    d.label,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp, fontSize = 9.sp),
                    color = if (selected) Color(0xFF14160F) else p.textDim,
                )
            }
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val p = LocalRzPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            IconPill(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Geri",
                onClick = onBack,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.displaySmall, color = p.text)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = p.textDim)
        }
        trailing?.invoke()
    }
}
