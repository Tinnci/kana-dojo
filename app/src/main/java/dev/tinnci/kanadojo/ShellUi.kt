package dev.tinnci.kanadojo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaTopBar(
    selectedScript: Script,
    reduceMotion: Boolean,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onSettingsOpen: () -> Unit,
    onScriptChange: (Script) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit
) {
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = {
            Column {
                Text(stringResource(R.string.app_name), fontWeight = FontWeight.Black)
                Text(stringResource(R.string.app_subtitle), style = MaterialTheme.typography.labelMedium)
            }
        },
        actions = {
            Row(modifier = Modifier.padding(end = 8.dp)) {
                IconButton(onClick = {
                    onSettingsOpen()
                    settingsOpen = true
                }) {
                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_content_description))
                }
                DropdownMenu(expanded = settingsOpen, onDismissRequest = { settingsOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_sound)) },
                        onClick = { onSoundEnabledChange(!soundEnabled) },
                        trailingIcon = {
                            Switch(checked = soundEnabled, onCheckedChange = onSoundEnabledChange)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_reduced_motion)) },
                        onClick = { onReduceMotionChange(!reduceMotion) },
                        trailingIcon = {
                            Switch(checked = reduceMotion, onCheckedChange = onReduceMotionChange)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_haptics)) },
                        onClick = { onHapticsEnabledChange(!hapticsEnabled) },
                        trailingIcon = {
                            Switch(checked = hapticsEnabled, onCheckedChange = onHapticsEnabledChange)
                        }
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(end = 8.dp)) {
                Script.entries.forEachIndexed { index, script ->
                    SegmentedButton(
                        selected = script == selectedScript,
                        onClick = { onScriptChange(script) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Script.entries.size),
                        label = {
                            Text(stringResource(script.shortNameResId))
                        }
                    )
                }
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
                        contentDescription = stringResource(tab.labelResId)
                    )
                },
                label = { Text(stringResource(tab.labelResId)) }
            )
        }
    }
}

private val ScreenTab.labelResId: Int
    get() = when (this) {
        ScreenTab.Lessons -> R.string.tab_lessons
        ScreenTab.Chart -> R.string.tab_chart
        ScreenTab.Mistakes -> R.string.tab_practice
    }
