package dev.mrbean.laundryledger.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mrbean.laundryledger.R
import dev.mrbean.laundryledger.data.BatchWithItems
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: LedgerViewModel, onOpenBatch: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val batches by vm.batches.collectAsState()
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.checkForUpdate() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Header(onOpenSettings)
        UpdateBanner(vm, onOpenSettings)
        Crossfade(batches == null, Modifier.weight(1f), label = "load") { loading ->
            if (loading) {
                Box(Modifier.fillMaxSize())
            } else {
                val all = batches.orEmpty()
                val active = all.filter { !it.isReturned }
                val done = all.filter { it.isReturned }
                if (all.isEmpty()) EmptyState()
                else LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (active.isNotEmpty()) {
                        item(key = "h_active") {
                            SectionTitle(if (active.size == 1) "At the laundry" else "At the laundry · ${active.size}", Modifier.animateItem())
                        }
                        items(active, key = { it.batch.id }) { b ->
                            ActiveBatchCard(b, Modifier.animateItem()) { onOpenBatch(b.batch.id) }
                        }
                    }
                    if (done.isNotEmpty()) {
                        item(key = "h_done") { SectionTitle("History", Modifier.animateItem().padding(top = 8.dp)) }
                        items(done, key = { it.batch.id }) { b ->
                            HistoryRow(b, Modifier.animateItem()) { onOpenBatch(b.batch.id) }
                        }
                    }
                }
            }
        }
        NewBatchButton(enabled = !creating) {
            creating = true
            scope.launch {
                val id = vm.createBatch()
                onOpenBatch(id)
                creating = false
            }
        }
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Laundry Ledger", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Every piece out, every piece back",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val src = rememberSource()
        Box(
            Modifier.size(48.dp).pressScale(src, 0.88f).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Settings, "Settings", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 4.dp, top = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveBatchCard(b: BatchWithItems, modifier: Modifier, onClick: () -> Unit) {
    val src = rememberSource()
    val progress by animateFloatAsState(
        if (b.totalSent == 0) 0f else b.totalReceived / b.totalSent.toFloat(), tween(600), label = "p",
    )
    Surface(
        onClick = onClick,
        interactionSource = src,
        modifier = modifier.fillMaxWidth().pressScale(src, 0.97f),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(friendlyDay(b.batch.createdAt), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(daysAgo(b.batch.createdAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${b.totalSent}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("pieces", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (b.items.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    b.items.sortedBy { it.clothType.ordinal }.forEach { item ->
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ClothIcon(item.clothType, 26.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("${item.sent}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)),
                    color = Success,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (b.totalReceived == 0) "Awaiting return" else "${b.totalReceived}/${b.totalSent} back",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(b: BatchWithItems, modifier: Modifier, onClick: () -> Unit) {
    val missing = b.totalSent - b.totalReceived
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.CheckCircle, null,
            tint = if (missing == 0) Success else Warning,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(friendlyDay(b.batch.createdAt), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                b.batch.returnedAt?.let { "Back " + friendlyDay(it).let { d -> if (d == "Today" || d == "Yesterday") d.lowercase() else "on $d" } }.orEmpty() +
                    if (missing > 0) " · $missing missing" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (missing > 0) Warning else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(pieces(b.totalSent), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState() {
    var shown by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(shown, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 6 }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.cloth_basket), null, Modifier.size(160.dp))
                Spacer(Modifier.height(20.dp))
                Text("No laundry out right now", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Start a new batch when you hand clothes to the laundry. Count them back in when they return.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun NewBatchButton(enabled: Boolean, onClick: () -> Unit) {
    val src = rememberSource()
    Surface(color = MaterialTheme.colorScheme.background) {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = src,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                .height(68.dp).pressScale(src),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
        ) {
            Icon(Icons.Rounded.Add, null, Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("New batch", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun UpdateBanner(vm: LedgerViewModel, onOpen: () -> Unit) {
    val state by vm.updater.state.collectAsState()
    val avail = state as? dev.mrbean.laundryledger.update.UpdateState.Available
    var dismissed by remember { mutableStateOf(vm.prefs.dismissedVersion) }
    val show = avail != null && dismissed != avail.release.version
    AnimatedVisibility(
        show,
        enter = androidx.compose.animation.expandVertically() + fadeIn(),
        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
    ) {
        val version = avail?.release?.version.orEmpty()
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth().clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer).clickable(onClick = onOpen)
                .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Update available: v$version", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Tap to see what's new and install", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }
            androidx.compose.material3.IconButton(onClick = { vm.dismissUpdate(version); dismissed = version }) {
                Icon(Icons.Rounded.Close, "Dismiss", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
