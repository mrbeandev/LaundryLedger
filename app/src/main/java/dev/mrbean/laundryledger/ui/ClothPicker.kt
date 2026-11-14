package dev.mrbean.laundryledger.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mrbean.laundryledger.data.ClothType
import dev.mrbean.laundryledger.data.Gender

fun Gender.icon(): ImageVector = when (this) {
    Gender.MALE -> Icons.Rounded.Male
    Gender.FEMALE -> Icons.Rounded.Female
    Gender.UNSPECIFIED -> Icons.Rounded.People
}

/** Large selectable card for a gender choice. */
@Composable
fun GenderOption(gender: Gender, selected: Boolean, subtitle: String, onClick: () -> Unit) {
    val src = rememberSource()
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        tween(220), label = "gbg",
    )
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        tween(220), label = "gborder",
    )
    Row(
        Modifier.fillMaxWidth().pressScale(src, 0.97f).clip(MaterialTheme.shapes.large).background(bg)
            .border(2.dp, border, MaterialTheme.shapes.large)
            .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                gender.icon(), null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(gender.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(selected, enter = scaleIn(), exit = scaleOut()) {
            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

fun Gender.subtitle() = when (this) {
    Gender.MALE -> "Shirts, banians, kurtas, lungis..."
    Gender.FEMALE -> "Sarees, kurtis, blouses, dupattas..."
    Gender.UNSPECIFIED -> "Show every cloth type"
}

/** Grid of cloth types with check marks; used by onboarding and settings. */
@Composable
fun ClothPickerGrid(
    catalog: List<ClothType>,
    selected: Set<ClothType>,
    onToggle: (ClothType) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    header: (@Composable () -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${selected.count { it in catalog }} of ${catalog.size} selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSelectAll) { Text("All") }
                TextButton(onClick = onClear) { Text("None") }
            }
        }
        items(catalog, key = { it.name }) { t ->
            PickTile(t, t in selected) { onToggle(t) }
        }
    }
}

@Composable
private fun PickTile(type: ClothType, checked: Boolean, onClick: () -> Unit) {
    val src = rememberSource()
    val bg by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        tween(200), label = "pbg",
    )
    val border by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        tween(200), label = "pborder",
    )
    val alpha by animateFloatAsState(if (checked) 1f else 0.55f, tween(200), label = "palpha")
    val iconScale by animateFloatAsState(
        if (checked) 1f else 0.9f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "pscale",
    )
    Box(
        Modifier.height(108.dp).pressScale(src, 0.92f).clip(RoundedCornerShape(20.dp)).background(bg)
            .border(2.dp, border, RoundedCornerShape(20.dp))
            .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ClothIcon(type, 54.dp, Modifier.graphicsLayer { this.alpha = alpha; scaleX = iconScale; scaleY = iconScale })
            Spacer(Modifier.height(6.dp))
            Text(
                type.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(
            checked, enter = scaleIn(), exit = scaleOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
        ) {
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Color.White) }
        }
    }
}
