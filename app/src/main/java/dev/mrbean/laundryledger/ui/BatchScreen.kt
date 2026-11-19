package dev.mrbean.laundryledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mrbean.laundryledger.data.BatchItem
import dev.mrbean.laundryledger.data.BatchWithItems
import dev.mrbean.laundryledger.data.ClothType
import kotlinx.coroutines.launch

private enum class Mode { GIVE, CHECK }

@Composable
fun BatchScreen(vm: LedgerViewModel, batchId: Long, onBack: () -> Unit) {
    val flow = remember(batchId) { vm.batch(batchId) }
    val loaded by flow.collectAsState(initial = null)
    // Keep last non-null value so the screen doesn't blank out while deleting / animating away.
    var last by remember { mutableStateOf<BatchWithItems?>(null) }
    if (loaded != null) last = loaded
    val data = last ?: run { Box(Modifier.fillMaxSize()); return }

    BackHandler(onBack = onBack)

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    var mode by rememberSaveable {
        mutableStateOf(if (data.isReturned || data.totalReceived > 0) Mode.CHECK else Mode.GIVE)
    }
    val enabled by vm.prefs.enabled.collectAsState()
    var selected by rememberSaveable {
        mutableStateOf(vm.prefs.lastType.takeIf { it in enabled } ?: enabled.firstOrNull() ?: ClothType.SHIRT)
    }
    var qty by rememberSaveable { mutableIntStateOf(1) }
    var flashType by remember { mutableStateOf<String?>(null) }
    var flashTick by remember { mutableLongStateOf(0L) }
    var confirmDelete by remember { mutableStateOf(false) }

    val items = data.items.sortedBy { it.clothType.ordinal }
    val listState = rememberLazyListState()

    LaunchedEffect(flashTick) {
        val t = flashType ?: return@LaunchedEffect
        val idx = items.indexOfFirst { it.type == t }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    fun removeWithUndo(item: BatchItem) {
        vm.setSent(item, 0)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(
                "Removed ${item.clothType.label}", actionLabel = "Undo", duration = SnackbarDuration.Short,
            )
            if (r == SnackbarResult.ActionPerformed) vm.restoreItem(item)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                data = data,
                onBack = onBack,
                onDelete = { confirmDelete = true },
                onToggleReturned = { vm.setReturned(batchId, !data.isReturned) },
            )
            ModeSwitch(mode, enabled = data.items.isNotEmpty() || mode == Mode.CHECK) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                mode = it
            }
            Box(Modifier.weight(1f)) {
                if (items.isEmpty()) {
                    EmptyBatchHint(Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            Box(Modifier.animateItem()) {
                                AnimatedContent(
                                    mode,
                                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                                    label = "row",
                                ) { m ->
                                    if (m == Mode.GIVE) GiveRow(
                                        item = item,
                                        flash = flashType == item.type,
                                        flashTick = flashTick,
                                        onMinus = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (item.sent <= 1) removeWithUndo(item) else vm.setSent(item, item.sent - 1)
                                        },
                                        onPlus = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            vm.setSent(item, item.sent + 1)
                                        },
                                        onClick = {
                                            selected = item.clothType
                                            qty = 1
                                        },
                                    ) else CheckRow(
                                        item = item,
                                        onTap = {
                                            if (item.received < item.sent) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                vm.setReceived(item, item.received + 1)
                                            }
                                        },
                                        onMinus = { vm.setReceived(item, item.received - 1) },
                                        onAll = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            vm.setReceived(item, item.sent)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (slideInVertically(tween(350)) { it / 2 } + fadeIn(tween(300))) togetherWith
                        (slideOutVertically(tween(250)) { it / 2 } + fadeOut(tween(200)))
                },
                label = "panel",
            ) { m ->
                if (m == Mode.GIVE) AddPanel(
                    types = enabled,
                    selected = selected,
                    qty = qty,
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selected = it
                        qty = 1
                    },
                    onQty = { qty = it.coerceIn(1, 99) },
                    onAdd = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.add(batchId, selected, qty)
                        qty = 1
                        flashType = selected.name
                        flashTick = System.nanoTime()
                    },
                ) else CheckPanel(
                    data = data,
                    onReceiveAll = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.receiveAll(batchId)
                    },
                    onFinish = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.setReturned(batchId, true)
                        onBack()
                    },
                    onReopen = { vm.setReturned(batchId, false) },
                )
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 300.dp),
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this batch?") },
            text = { Text("All ${pieces(data.totalSent)} recorded in it will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteBatch(batchId)
                    onBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TopBar(data: BatchWithItems, onBack: () -> Unit, onDelete: () -> Unit, onToggleReturned: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                friendlyDateTime(data.batch.createdAt),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (data.isReturned) "Returned ${friendlyDay(data.batch.returnedAt!!)}" else "At the laundry",
                style = MaterialTheme.typography.bodySmall,
                color = if (data.isReturned) Success else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
            RollingNumber(
                data.totalSent,
                MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                MaterialTheme.colorScheme.primary,
            )
            Text("total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (data.items.isNotEmpty()) DropdownMenuItem(
                    text = { Text(if (data.isReturned) "Mark as still at laundry" else "Mark as returned") },
                    leadingIcon = { Icon(if (data.isReturned) Icons.Rounded.Undo else Icons.Rounded.DoneAll, null) },
                    onClick = { menu = false; onToggleReturned() },
                )
                DropdownMenuItem(
                    text = { Text("Delete batch", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun ModeSwitch(mode: Mode, enabled: Boolean, onChange: (Mode) -> Unit) {
    AnimatedVisibility(enabled, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Box(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(44.dp)
                .clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
        ) {
            val offset by animateFloatAsState(
                if (mode == Mode.GIVE) 0f else 1f,
                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "seg",
            )
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val half = maxWidth / 2
                Box(
                    Modifier.width(half).fillMaxSize().graphicsLayer { translationX = offset * half.toPx() }
                        .shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                )
                Row(Modifier.fillMaxSize()) {
                    listOf(Mode.GIVE to "Giving", Mode.CHECK to "Checking back").forEach { (m, label) ->
                        val c by animateColorAsState(
                            if (mode == m) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "c",
                        )
                        Box(
                            Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(50))
                                .clickable { if (mode != m) onChange(m) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = c, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyBatchHint(modifier: Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nothing added yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Pick a cloth type below, set how many, and tap Add.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GiveRow(
    item: BatchItem, flash: Boolean, flashTick: Long,
    onMinus: () -> Unit, onPlus: () -> Unit, onClick: () -> Unit,
) {
    val base = MaterialTheme.colorScheme.surfaceContainerLowest
    val highlight = MaterialTheme.colorScheme.primaryContainer
    var lit by remember { mutableStateOf(false) }
    LaunchedEffect(flashTick) {
        if (flash) { lit = true; kotlinx.coroutines.delay(450); lit = false }
    }
    val bg by animateColorAsState(if (lit) highlight else base, tween(if (lit) 120 else 700), label = "flash")
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(bg)
            .clickable(onClick = onClick).padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClothBubble(item.clothType, 42.dp, MaterialTheme.colorScheme.surfaceContainer)
        Spacer(Modifier.width(12.dp))
        Text(item.clothType.label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        SmallRoundButton(Icons.Rounded.Remove, "Less", onMinus)
        RollingNumber(
            item.sent,
            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            modifier = Modifier.width(40.dp),
        )
        SmallRoundButton(Icons.Rounded.Add, "More", onPlus)
    }
}

@Composable
private fun SmallRoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    val src = rememberSource()
    Box(
        Modifier.size(40.dp).pressScale(src, 0.85f).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(20.dp)) }
}

@Composable
private fun CheckRow(item: BatchItem, onTap: () -> Unit, onMinus: () -> Unit, onAll: () -> Unit) {
    val complete = item.received >= item.sent
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val bg by animateColorAsState(
        if (complete) (if (dark) Color(0xFF173A28) else SuccessContainer) else MaterialTheme.colorScheme.surfaceContainerLowest,
        tween(400), label = "bg",
    )
    val fg = if (complete) (if (dark) Color(0xFFBDF0D0) else Color(0xFF14502E)) else MaterialTheme.colorScheme.onSurface
    val src = rememberSource()
    Row(
        Modifier.fillMaxWidth().pressScale(src, 0.98f).clip(MaterialTheme.shapes.medium).background(bg)
            .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onTap)
            .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClothBubble(item.clothType, 46.dp, if (complete) Color.White.copy(alpha = if (dark) 0.12f else 0.6f) else MaterialTheme.colorScheme.surfaceContainer)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.clothType.label, style = MaterialTheme.typography.titleMedium, color = fg)
            Text(
                if (complete) "All back" else "${item.sent - item.received} to come",
                style = MaterialTheme.typography.bodySmall,
                color = if (complete) (if (dark) Color(0xFF6FD69A) else Success) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.received > 0) {
            IconButton(onClick = onMinus, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Remove, "Uncount", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 6.dp)) {
            RollingNumber(item.received, MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), fg)
            Text("/${item.sent}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 1.dp))
        }
        AnimatedContent(complete, label = "done") { c ->
            if (c) Box(Modifier.size(36.dp).clip(CircleShape).background(Success), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
            } else Box(
                Modifier.size(36.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable(onClick = onAll),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.DoneAll, "All back", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun PanelSurface(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddPanel(
    types: List<ClothType>,
    selected: ClothType, qty: Int,
    onSelect: (ClothType) -> Unit, onQty: (Int) -> Unit, onAdd: () -> Unit,
) {
    PanelSurface {
        // 4-column grid of the user's cloth types. Up to 3 rows fit; more scroll inside the panel.
        val rows = types.chunked(4)
        val maxRows = 3
        val tileH = 84.dp
        val gap = 8.dp
        Column(
            Modifier.fillMaxWidth()
                .then(
                    if (rows.size > maxRows) Modifier.height(tileH * maxRows + gap * maxRows + tileH / 3)
                    else Modifier,
                )
                .verticalScroll(rememberScrollState()),
        ) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { t -> TypeTile(t, t == selected, Modifier.weight(1f)) { onSelect(t) } }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(gap))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Stepper
            Row(
                Modifier.height(64.dp).clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepButton(Icons.Rounded.Remove, "Fewer", enabled = qty > 1) { onQty(qty - 1) }
                RollingNumber(
                    qty,
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    modifier = Modifier.width(52.dp),
                )
                StepButton(Icons.Rounded.Add, "More", enabled = qty < 99) { onQty(qty + 1) }
            }
            Spacer(Modifier.width(10.dp))
            val src = rememberSource()
            Button(
                onClick = onAdd,
                interactionSource = src,
                modifier = Modifier.weight(1f).height(64.dp).pressScale(src),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                AnimatedContent(
                    selected,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                    },
                    label = "addLabel",
                ) { t ->
                    Column {
                        Text("Add $qty", fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
                        Text(
                            t.label, fontSize = 13.sp, lineHeight = 15.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val src = rememberSource()
    Box(
        Modifier.size(56.dp).pressScale(src, 0.82f).clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, desc, Modifier.size(28.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun TypeTile(type: ClothType, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val src = rememberSource()
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        tween(220), label = "tileBg",
    )
    val borderC by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, tween(220), label = "tileBorder",
    )
    val iconScale by animateFloatAsState(
        if (selected) 1.12f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "iconScale",
    )
    Column(
        modifier.height(84.dp).pressScale(src, 0.92f).clip(RoundedCornerShape(18.dp)).background(bg)
            .border(2.dp, borderC, RoundedCornerShape(18.dp))
            .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ClothIcon(type, 44.dp, Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale })
        Text(
            type.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CheckPanel(data: BatchWithItems, onReceiveAll: () -> Unit, onFinish: () -> Unit, onReopen: () -> Unit) {
    val missing = data.totalSent - data.totalReceived
    val allBack = missing == 0 && data.totalSent > 0
    PanelSurface {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
            Column(Modifier.weight(1f)) {
                AnimatedContent(allBack, label = "status") { ok ->
                    Text(
                        if (ok) "Everything is back" else "$missing still missing",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = if (ok) Success else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    "${data.totalReceived} of ${data.totalSent} counted back · tap a row per piece",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        val p by animateFloatAsState(
            if (data.totalSent == 0) 0f else data.totalReceived / data.totalSent.toFloat(), tween(500), label = "p",
        )
        androidx.compose.material3.LinearProgressIndicator(
            progress = { p },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(10.dp).clip(RoundedCornerShape(50)),
            color = Success, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
        )
        Spacer(Modifier.height(14.dp))
        if (data.isReturned) {
            FilledTonalButton(onClick = onReopen, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(20.dp)) {
                Icon(Icons.Rounded.Undo, null); Spacer(Modifier.width(8.dp))
                Text("Mark as still at laundry", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row {
                if (!allBack) {
                    FilledTonalButton(
                        onClick = onReceiveAll,
                        modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(20.dp),
                    ) { Text("All arrived", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.width(10.dp))
                }
                val src = rememberSource()
                Button(
                    onClick = onFinish,
                    interactionSource = src,
                    modifier = Modifier.weight(1f).height(64.dp).pressScale(src),
                    shape = RoundedCornerShape(20.dp),
                    colors = if (allBack) ButtonDefaults.buttonColors(containerColor = Success, contentColor = Color.White)
                    else ButtonDefaults.buttonColors(),
                ) {
                    Icon(Icons.Rounded.DoneAll, null); Spacer(Modifier.width(8.dp))
                    Text(if (allBack) "Close batch" else "Close", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
