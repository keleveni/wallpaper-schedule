package com.keleveni.wallpaperscheduler.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keleveni.wallpaperscheduler.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val autoChangeEnabled by viewModel.autoChangeEnabled.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_auto_change)) },
                supportingContent = { Text(stringResource(R.string.settings_auto_change_desc)) },
                trailingContent = {
                    Switch(checked = autoChangeEnabled, onCheckedChange = viewModel::setAutoChangeEnabled)
                },
            )
            HorizontalDivider()

            Text(
                stringResource(R.string.settings_permissions),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
            PermissionItem(
                title = stringResource(R.string.settings_exact_alarm),
                description = stringResource(R.string.settings_exact_alarm_desc),
                granted = permissions.canScheduleExactAlarms,
                onRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                } else {
                    null
                },
            )
            PermissionItem(
                title = stringResource(R.string.settings_battery),
                description = stringResource(R.string.settings_battery_desc),
                granted = permissions.ignoringBatteryOptimizations,
                onRequest = {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                },
            )
            if (!permissions.wallpaperChangeAllowed) {
                PermissionItem(
                    title = stringResource(R.string.settings_wallpaper_permission),
                    description = stringResource(R.string.settings_wallpaper_permission_desc),
                    granted = false,
                    onRequest = null,
                )
            }

            Card(Modifier.padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_about_delay_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_about_delay),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: (() -> Unit)?,
) {
    ListItem(
        leadingContent = {
            if (granted) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        },
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text(description)
                Text(
                    stringResource(if (granted) R.string.status_granted else R.string.status_denied),
                    color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        },
        trailingContent = if (!granted && onRequest != null) {
            { TextButton(onClick = onRequest) { Text(stringResource(R.string.action_open_settings)) } }
        } else {
            null
        },
    )
}
