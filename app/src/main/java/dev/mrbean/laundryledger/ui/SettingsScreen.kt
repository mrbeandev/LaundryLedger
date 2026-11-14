package dev.mrbean.laundryledger.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mrbean.laundryledger.data.ClothType
import dev.mrbean.laundryledger.update.UpdateState
import dev.mrbean.laundryledger.update.Updater
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: LedgerViewModel, onBack: () -> Unit, onEditClothes: () -> Unit) {
    val gender by vm.prefs.gender.collectAsState()
    val enabled by vm.prefs.enabled.collectAsState()
    val autoUpdate by vm.prefs.autoUpdate.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar("Settings", onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SettingsLabel("Your clothes") }
            item {
                Card {
                    Text("Clothes for", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    GenderSegments(gender) { vm.changeGender(it) }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Changing this adds the usual clothes for that choice to your list. Nothing is removed.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                NavRow(
                    icon = Icons.Rounded.Checkroom,
                    title = "Cloth types",
                    subtitle = "${enabled.size} shown when adding clothes",
                    onClick = onEditClothes,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                        enabled.take(4).forEach { ClothBubble(it, 30.dp, MaterialTheme.colorScheme.surfaceContainer) }
                    }
                }
            }

            item { SettingsLabel("Updates") }
            item { UpdateCard(vm) }
            item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Check for updates automatically", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Once a day, when you open the app. Only asks GitHub for the latest version number.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(autoUpdate, onCheckedChange = { vm.prefs.setAutoUpdate(it) })
                    }
                }
            }

            item { SettingsLabel("Privacy") }
            item { PrivacyNote() }

            item { SettingsLabel("About") }
            item {
                NavRow(
                    icon = Icons.Rounded.Code,
                    title = "Source code",
                    subtitle = "Free and open source on GitHub",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Updater.REPO_URL)))
                    },
                )
            }
            item {
                Text(
                    "Laundry Ledger v${vm.updater.currentVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun ClothTypesScreen(vm: LedgerViewModel, onBack: () -> Unit) {
    val gender by vm.prefs.gender.collectAsState()
    val enabled by vm.prefs.enabled.collectAsState()
    val catalog = remember(gender) { ClothType.catalog(gender) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar("Cloth types", onBack)
        ClothPickerGrid(
            catalog = catalog,
            selected = enabled.toSet(),
            onToggle = { t ->
                val s = enabled.toSet()
                // Keep at least one type so the add panel is never empty.
                if (t in s && s.size == 1) return@ClothPickerGrid
                vm.prefs.setEnabled(if (t in s) s - t else s + t)
            },
            onSelectAll = { vm.prefs.setEnabled(enabled.toSet() + catalog) },
            onClear = { vm.prefs.setEnabled(setOf(enabled.firstOrNull() ?: catalog.first())) },
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 32.dp),
            header = {
                Text(
                    "Tap to show or hide a cloth type in the add panel. Batches you already saved are not affected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            },
        )
    }
}

@Composable
fun SimpleTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(18.dp)) { content() } }
}

@Composable
private fun NavRow(
    icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GenderSegments(selected: dev.mrbean.laundryledger.data.Gender, onPick: (dev.mrbean.laundryledger.data.Gender) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        dev.mrbean.laundryledger.data.Gender.entries.forEach { g ->
            val on = g == selected
            val bg by androidx.compose.animation.animateColorAsState(
                if (on) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, label = "seg",
            )
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(bg)
                    .clickable { onPick(g) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    g.icon(), null, Modifier.size(20.dp),
                    tint = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (g == dev.mrbean.laundryledger.data.Gender.UNSPECIFIED) "Not saying" else g.label,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun UpdateCard(vm: LedgerViewModel) {
    val state by vm.updater.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var needPermission by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Version ${vm.updater.currentVersion}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    AnimatedContent(state, contentKey = { it::class }, label = "ustate") { s ->
                        Text(
                            when (s) {
                                UpdateState.Idle -> "Tap check to look for a newer version"
                                UpdateState.Checking -> "Checking GitHub..."
                                UpdateState.UpToDate -> "You're on the latest version"
                                is UpdateState.Available -> "Version ${s.release.version} is available"
                                is UpdateState.Downloading -> "Downloading ${s.release.version}... ${(s.progress * 100).toInt()}%"
                                is UpdateState.Installing -> "Installing..."
                                is UpdateState.Failed -> s.message
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (s) {
                                is UpdateState.Available -> MaterialTheme.colorScheme.primary
                                is UpdateState.Failed -> MaterialTheme.colorScheme.error
                                UpdateState.UpToDate -> Success
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            val s = state
            if (s is UpdateState.Available && s.release.notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    s.release.notes.lines().filter { it.isNotBlank() }.take(8).joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp),
                )
            }
            if (s is UpdateState.Downloading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { s.progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
                )
            }
            if (needPermission) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "To install updates, allow Laundry Ledger to install apps, then come back and tap Update again.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            val release = when (s) {
                is UpdateState.Available -> s.release
                is UpdateState.Failed -> s.release
                else -> null
            }
            if (release != null) {
                Button(
                    onClick = {
                        if (!vm.updater.canInstall()) {
                            needPermission = true
                            context.startActivity(vm.updater.installPermissionIntent())
                        } else {
                            needPermission = false
                            scope.launch { vm.updater.downloadAndInstall(release) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text("Update to ${release.version}", fontWeight = FontWeight.SemiBold) }
            } else {
                val busy = s is UpdateState.Checking || s is UpdateState.Downloading || s is UpdateState.Installing
                FilledTonalButton(
                    onClick = { scope.launch { vm.checkForUpdate(manual = true) } },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text("Check for updates", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
