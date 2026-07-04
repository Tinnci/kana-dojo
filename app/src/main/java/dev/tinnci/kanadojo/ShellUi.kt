package dev.tinnci.kanadojo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaTopBar(
    selectedScript: Script,
    reduceMotion: Boolean,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onScriptChange: (Script) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = {
            Column {
                Text("Kana Dojo", fontWeight = FontWeight.Black)
                Text("guided kana drills", style = MaterialTheme.typography.labelMedium)
            }
        },
        actions = {
            Box {
                AssistChip(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = { settingsOpen = true },
                    label = { Text("Settings") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                DropdownMenu(expanded = settingsOpen, onDismissRequest = { settingsOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Sound") },
                        onClick = { onSoundEnabledChange(!soundEnabled) },
                        trailingIcon = {
                            Switch(checked = soundEnabled, onCheckedChange = onSoundEnabledChange)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reduced motion") },
                        onClick = { onReduceMotionChange(!reduceMotion) },
                        trailingIcon = {
                            Switch(checked = reduceMotion, onCheckedChange = onReduceMotionChange)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Haptics") },
                        onClick = { onHapticsEnabledChange(!hapticsEnabled) },
                        trailingIcon = {
                            Switch(checked = hapticsEnabled, onCheckedChange = onHapticsEnabledChange)
                        }
                    )
                }
            }
            Script.entries.forEach { script ->
                AssistChip(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = { onScriptChange(script) },
                    label = { Text(script.label) },
                    leadingIcon = {
                        if (script == selectedScript) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }
        }
    )
}

@Composable
fun KanaBottomBar(currentTab: ScreenTab, onTabChange: (ScreenTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        ScreenTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabChange(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            ScreenTab.Lessons -> Icons.Outlined.School
                            ScreenTab.Chart -> Icons.Outlined.GridView
                            ScreenTab.Mistakes -> Icons.Outlined.Replay
                        },
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) }
            )
        }
    }
}
