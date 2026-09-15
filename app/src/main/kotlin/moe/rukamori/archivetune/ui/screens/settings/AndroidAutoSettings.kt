/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package moe.rukamori.archivetune.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.androidauto.AndroidAutoActionSlot
import moe.rukamori.archivetune.androidauto.AndroidAutoConnectionStatus
import moe.rukamori.archivetune.androidauto.AndroidAutoCustomAction
import moe.rukamori.archivetune.androidauto.AndroidAutoSettingsSnapshot
import moe.rukamori.archivetune.viewmodels.AndroidAutoSettingsAction
import moe.rukamori.archivetune.viewmodels.AndroidAutoSettingsEvent
import moe.rukamori.archivetune.viewmodels.AndroidAutoSettingsState
import moe.rukamori.archivetune.viewmodels.AndroidAutoSettingsUiModel
import moe.rukamori.archivetune.viewmodels.AndroidAutoSettingsViewModel

@Composable
fun AndroidAutoSettings(
    navController: NavController,
    viewModel: AndroidAutoSettingsViewModel = hiltViewModel(),
) {
    val onBack = remember(navController) { { navController.navigateUp(); Unit } }
    AndroidAutoSettingsRoute(onBack = onBack, viewModel = viewModel)
}

@Composable
fun AndroidAutoSettingsRoute(
    onBack: () -> Unit,
    viewModel: AndroidAutoSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onAction = remember(viewModel) { viewModel::onAction }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onAction(AndroidAutoSettingsAction.PermissionResult) }
    LaunchedEffect(viewModel, context, permissionLauncher) {
        viewModel.events.collectLatest { event ->
            when (event) {
                AndroidAutoSettingsEvent.RequestAudioPermission -> permissionLauncher.launch(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                )
                AndroidAutoSettingsEvent.OpenAppPermissions -> runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                }.onFailure {
                    onAction(AndroidAutoSettingsAction.ExternalActionFailed)
                }
            }
        }
    }
    AndroidAutoSettingsContent(state = state, onAction = onAction, onBack = onBack)
}

@Composable
private fun AndroidAutoSettingsContent(
    state: AndroidAutoSettingsState,
    onAction: (AndroidAutoSettingsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.android_auto)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back_button_desc))
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            AndroidAutoSettingsState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            is AndroidAutoSettingsState.Success -> AndroidAutoSettingsBody(
                model = state.model,
                onAction = onAction,
                modifier = Modifier.padding(padding),
            )
            AndroidAutoSettingsState.Empty -> AndroidAutoSettingsFailure(onAction, Modifier.padding(padding))
            is AndroidAutoSettingsState.Error -> AndroidAutoSettingsFailure(
                onAction = onAction,
                modifier = Modifier.padding(padding),
                messageRes = state.messageRes,
            )
        }
    }
}

@Composable
private fun AndroidAutoSettingsBody(
    model: AndroidAutoSettingsUiModel,
    onAction: (AndroidAutoSettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = model.snapshot.configuration
    val contentModifier = remember(modifier) {
        modifier.fillMaxSize().padding(horizontal = SettingsDimensions.ScreenHorizontalPadding)
            .padding(top = SettingsDimensions.SectionSpacing, bottom = SettingsDimensions.ScreenBottomPadding)
    }
    val verticalArrangement = remember { Arrangement.spacedBy(SettingsDimensions.SectionSpacing) }
    Column(
        modifier = contentModifier.verticalScroll(rememberScrollState()),
        verticalArrangement = verticalArrangement,
    ) {
        AndroidAutoConnectionCard(model.snapshot, onAction)
        AndroidAutoSection(stringResource(R.string.android_auto_content)) {
            AndroidAutoSwitchRow(
                title = stringResource(R.string.android_auto_online_recommendations),
                description = stringResource(R.string.android_auto_online_recommendations_desc),
                checked = configuration.onlineRecommendations,
                enabled = !model.busy,
                onCheckedChange = remember(onAction) {
                    { onAction(AndroidAutoSettingsAction.SetOnlineRecommendations(it)) }
                },
            )
            HorizontalDivider()
            AndroidAutoSwitchRow(
                title = stringResource(R.string.android_auto_online_voice_search),
                description = stringResource(R.string.android_auto_online_voice_search_desc),
                checked = configuration.onlineVoiceSearch,
                enabled = !model.busy,
                onCheckedChange = remember(onAction) { { onAction(AndroidAutoSettingsAction.SetOnlineVoiceSearch(it)) } },
            )
            HorizontalDivider()
            AndroidAutoSwitchRow(
                title = stringResource(R.string.android_auto_local_songs),
                description = if (configuration.localSongs && !model.snapshot.hasLocalAudioPermission) {
                    stringResource(R.string.android_auto_audio_permission_missing)
                } else {
                    stringResource(R.string.android_auto_local_songs_desc)
                },
                checked = configuration.localSongs,
                enabled = !model.busy,
                onCheckedChange = remember(onAction, model.snapshot.hasLocalAudioPermission) { { enabled ->
                    onAction(AndroidAutoSettingsAction.SetLocalSongs(enabled))
                    if (enabled && !model.snapshot.hasLocalAudioPermission) {
                        onAction(AndroidAutoSettingsAction.RequestAudioPermission)
                    }
                } },
            )
        }
        AndroidAutoSection(stringResource(R.string.android_auto_data)) {
            AndroidAutoSwitchRow(
                title = stringResource(R.string.android_auto_metered_playback),
                description = stringResource(R.string.android_auto_metered_playback_desc),
                checked = configuration.meteredPlayback,
                enabled = !model.busy,
                onCheckedChange = remember(onAction) { { onAction(AndroidAutoSettingsAction.SetMeteredPlayback(it)) } },
            )
            HorizontalDivider()
            AndroidAutoSwitchRow(
                title = stringResource(R.string.android_auto_metered_artwork),
                description = stringResource(R.string.android_auto_metered_artwork_desc),
                checked = configuration.meteredArtwork,
                enabled = !model.busy,
                onCheckedChange = remember(onAction) { { onAction(AndroidAutoSettingsAction.SetMeteredArtwork(it)) } },
            )
        }
        AndroidAutoSection(stringResource(R.string.android_auto_controls)) {
            AndroidAutoValueRow(
                title = stringResource(R.string.android_auto_primary_action),
                value = stringResource(configuration.primaryAction.labelResource()),
                enabled = !model.busy,
                onClick = remember(onAction) {
                    { onAction(AndroidAutoSettingsAction.ShowActionPicker(AndroidAutoActionSlot.PRIMARY)) }
                },
            )
            HorizontalDivider()
            AndroidAutoValueRow(
                title = stringResource(R.string.android_auto_secondary_action),
                value = stringResource(configuration.secondaryAction.labelResource()),
                enabled = !model.busy,
                onClick = remember(onAction) {
                    { onAction(AndroidAutoSettingsAction.ShowActionPicker(AndroidAutoActionSlot.SECONDARY)) }
                },
            )
        }
    }
    model.actionDialog?.let { slot ->
        AndroidAutoActionDialog(
            slot = slot,
            selected = if (slot == AndroidAutoActionSlot.PRIMARY) configuration.primaryAction else configuration.secondaryAction,
            onAction = onAction,
        )
    }
}

@Composable
private fun AndroidAutoConnectionCard(
    snapshot: AndroidAutoSettingsSnapshot,
    onAction: (AndroidAutoSettingsAction) -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
        val cardModifier = remember {
            Modifier.fillMaxWidth().padding(SettingsDimensions.RowHorizontalPadding)
        }
        Column(cardModifier) {
            Text(
                stringResource(R.string.android_auto_connection),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(
                    when (snapshot.connectionStatus) {
                        AndroidAutoConnectionStatus.DISCONNECTED -> R.string.android_auto_disconnected
                        AndroidAutoConnectionStatus.PROJECTION -> R.string.android_auto_projection
                        AndroidAutoConnectionStatus.NATIVE -> R.string.android_auto_native
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = remember(onAction) { { onAction(AndroidAutoSettingsAction.OpenAppPermissions) } },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.android_auto_app_permissions)) }
        }
    }
}

@Composable
private fun AndroidAutoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val arrangement = remember { Arrangement.spacedBy(SettingsDimensions.SectionHeaderBottomPadding) }
    val titleModifier = remember {
        Modifier.padding(horizontal = SettingsDimensions.RowHorizontalPadding).semantics { heading() }
    }
    Column(verticalArrangement = arrangement) {
        Text(
            title,
            modifier = titleModifier,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun AndroidAutoSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val rowModifier = remember(checked, enabled, onCheckedChange) {
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(
                horizontal = SettingsDimensions.RowHorizontalPadding,
                vertical = SettingsDimensions.RowVerticalPadding,
            )
    }
    val arrangement = remember { Arrangement.spacedBy(16.dp) }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = arrangement,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun AndroidAutoValueRow(
    title: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val rowModifier = remember(enabled, onClick) {
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .selectable(selected = false, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(
                horizontal = SettingsDimensions.RowHorizontalPadding,
                vertical = SettingsDimensions.RowVerticalPadding,
            )
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AndroidAutoActionDialog(
    slot: AndroidAutoActionSlot,
    selected: AndroidAutoCustomAction,
    onAction: (AndroidAutoSettingsAction) -> Unit,
) {
    val actions = remember(slot) {
        AndroidAutoCustomAction.entries.filter { slot == AndroidAutoActionSlot.SECONDARY || it != AndroidAutoCustomAction.NONE }
    }
    AlertDialog(
        onDismissRequest = remember(onAction) { { onAction(AndroidAutoSettingsAction.DismissActionPicker) } },
        title = {
            Text(
                stringResource(
                    if (slot == AndroidAutoActionSlot.PRIMARY) {
                        R.string.android_auto_primary_action
                    } else {
                        R.string.android_auto_secondary_action
                    },
                ),
            )
        },
        text = {
            Column(Modifier.selectableGroup()) {
                actions.forEach { action ->
                    val rowModifier = remember(action, selected, onAction) {
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                            selected = selected == action,
                            role = Role.RadioButton,
                            onClick = { onAction(AndroidAutoSettingsAction.SelectAction(action)) },
                        )
                    }
                    Row(
                        rowModifier,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == action, onClick = null)
                        Text(stringResource(action.labelResource()), Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = remember(onAction) { { onAction(AndroidAutoSettingsAction.DismissActionPicker) } }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun AndroidAutoSettingsFailure(
    onAction: (AndroidAutoSettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes messageRes: Int = R.string.android_auto_settings_load_failed,
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(messageRes), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = remember(onAction) { { onAction(AndroidAutoSettingsAction.Retry) } }) {
            Text(stringResource(R.string.retry_button))
        }
    }
}

@StringRes
private fun AndroidAutoCustomAction.labelResource(): Int = when (this) {
    AndroidAutoCustomAction.LIKE -> R.string.android_auto_action_like
    AndroidAutoCustomAction.START_RADIO -> R.string.android_auto_action_radio
    AndroidAutoCustomAction.SHUFFLE -> R.string.android_auto_action_shuffle
    AndroidAutoCustomAction.REPEAT -> R.string.android_auto_action_repeat
    AndroidAutoCustomAction.NONE -> R.string.android_auto_action_none
}
