@file:OptIn(ExperimentalMaterial3Api::class)

package com.oppzippy.openscq30.ui.devicesettings.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.ui.draw.clip
import com.oppzippy.openscq30.ui.theme.CardShapeMedium
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oppzippy.openscq30.R
import com.oppzippy.openscq30.lib.bindings.translateSettingId
import com.oppzippy.openscq30.lib.bindings.translateValue
import com.oppzippy.openscq30.lib.wrapper.QuickPreset
import com.oppzippy.openscq30.lib.wrapper.QuickPresetField
import com.oppzippy.openscq30.lib.wrapper.Select
import com.oppzippy.openscq30.lib.wrapper.Setting
import com.oppzippy.openscq30.lib.wrapper.toValue
import com.oppzippy.openscq30.ui.theme.OpenSCQ30Theme

@Composable
fun QuickPresetsScreen(
    quickPresets: List<QuickPreset>,
    onActivate: (String) -> Unit,
    onCreate: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Scaffold(
        floatingActionButton = { CreateQuickPresetFloatingButton(onCreate = onCreate) },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            QuickPresetsList(
                quickPresets = quickPresets,
                onActivate = onActivate,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun QuickPresetsList(
    quickPresets: List<QuickPreset>,
    onActivate: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var deleteDialog by remember { mutableStateOf<String?>(null) }
    deleteDialog?.let { currentDeleteDialog ->
        AlertDialog(
            onDismissRequest = { deleteDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(currentDeleteDialog)
                        deleteDialog = null
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.delete_quick_preset)) },
            text = { Text(stringResource(R.string.delete_confirm, currentDeleteDialog)) },
        )
    }

    if (quickPresets.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(quickPresets) { preset ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShapeMedium,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.bolt_24px),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledTonalButton(
                                onClick = { onActivate(preset.name) },
                            ) {
                                Text(stringResource(R.string.activate))
                            }
                            IconButton(onClick = { onEdit(preset.name) }) {
                                Icon(
                                    painter = painterResource(R.drawable.settings_24px),
                                    contentDescription = stringResource(R.string.edit),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { deleteDialog = preset.name }) {
                                Icon(
                                    painter = painterResource(R.drawable.remove_24px),
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bolt_24px),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                Text(
                    text = stringResource(R.string.no_quick_presets),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CreateQuickPresetFloatingButton(onCreate: (String) -> Unit) {
    var isDialogShown by remember { mutableStateOf(false) }
    if (isDialogShown) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { isDialogShown = false },
            confirmButton = {
                Button(
                    onClick = {
                        isDialogShown = false
                        onCreate(name)
                    },
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDialogShown = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.create_quick_preset)) },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                )
            },
        )
    }

    FloatingActionButton(
        onClick = { isDialogShown = true },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(
            painter = painterResource(R.drawable.add_24px),
            contentDescription = stringResource(R.string.create),
        )
    }
}

@Composable
fun EditQuickPresetPage(
    settings: Map<String, Setting>,
    quickPreset: QuickPreset,
    onToggleSetting: (name: String, setting: String, isEnabled: Boolean) -> Unit,
    onLoadCurrentSettings: () -> Unit,
) {
    var isInfoDialogOpen by remember { mutableStateOf(false) }
    if (isInfoDialogOpen) {
        AlertDialog(
            onDismissRequest = { isInfoDialogOpen = false },
            confirmButton = {},
            dismissButton = { Button(onClick = { isInfoDialogOpen = false }) { Text(stringResource(R.string.close)) } },
            title = { Text(stringResource(R.string.load_current_settings)) },
            text = { Text(stringResource(R.string.load_current_settings_help)) },
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        item {
            Row {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onLoadCurrentSettings,
                ) { Text(stringResource(R.string.load_current_settings)) }
                IconButton(onClick = { isInfoDialogOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.info_24px),
                        contentDescription = stringResource(R.string.information),
                    )
                }
            }
        }
        items(quickPreset.fields) { field ->
            Card(onClick = { onToggleSetting(quickPreset.name, field.settingId, !field.isEnabled) }) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = translateSettingId(field.settingId),
                            fontWeight = FontWeight.Bold,
                        )
                        Switch(
                            checked = field.isEnabled,
                            onCheckedChange = null,
                        )
                    }
                    Text(
                        text = translateValue(settings[field.settingId], field.value),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewQuickPresetsList() {
    OpenSCQ30Theme {
        QuickPresetsList(
            quickPresets = listOf(
                QuickPreset(name = "Preset 1", fields = emptyList()),
                QuickPreset(name = "Preset 2", fields = emptyList()),
                QuickPreset(name = "Preset 3", fields = emptyList()),
            ),
            onEdit = {},
            onActivate = {},
            onDelete = {},
        )
    }
}

@Preview
@Composable
private fun PreviewEditQuickPresetPage() {
    OpenSCQ30Theme {
        EditQuickPresetPage(
            settings = mapOf(
                Pair(
                    "test",
                    Setting.SelectSetting(
                        setting = Select(
                            options = listOf("a", "b", "c"),
                            localizedOptions = listOf("A", "B", "C"),
                        ),
                        value = "b",
                    ),
                ),
            ),
            quickPreset = QuickPreset(
                name = "Preset 1",
                fields = listOf(QuickPresetField("test", "b".toValue(), true)),
            ),
            onToggleSetting = { _, _, _ -> },
            onLoadCurrentSettings = {},
        )
    }
}
