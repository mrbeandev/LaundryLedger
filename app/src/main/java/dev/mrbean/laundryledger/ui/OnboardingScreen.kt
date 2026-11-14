package dev.mrbean.laundryledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mrbean.laundryledger.R
import dev.mrbean.laundryledger.data.ClothType
import dev.mrbean.laundryledger.data.Gender

@Composable
fun OnboardingScreen(onDone: (Gender, Set<ClothType>) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var gender by rememberSaveable { mutableStateOf<Gender?>(null) }
    var picked by rememberSaveable { mutableStateOf(setOf<ClothType>()) }

    BackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        StepDots(step, Modifier.statusBarsPadding().padding(top = 16.dp).align(Alignment.CenterHorizontally))
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(380)) { dir * it / 3 } + fadeIn(tween(380))) togetherWith
                    (slideOutHorizontally(tween(380)) { -dir * it / 3 } + fadeOut(tween(200)))
            },
            label = "onboarding",
        ) { s ->
            when (s) {
                0 -> WelcomeStep()
                1 -> GenderStep(gender) { g ->
                    if (g != gender) picked = ClothType.defaults(g)
                    gender = g
                }
                else -> {
                    val catalog = ClothType.catalog(gender ?: Gender.UNSPECIFIED)
                    ClothPickerGrid(
                        catalog = catalog,
                        selected = picked,
                        onToggle = { t -> picked = if (t in picked) picked - t else picked + t },
                        onSelectAll = { picked = catalog.toSet() },
                        onClear = { picked = emptySet() },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        header = {
                            StepTitle(
                                "What do you usually send?",
                                "Only these will show up when you add clothes. You can change this any time in Settings.",
                            )
                        },
                    )
                }
            }
        }
        val canContinue = when (step) {
            1 -> gender != null
            2 -> picked.isNotEmpty()
            else -> true
        }
        Surface(color = MaterialTheme.colorScheme.background) {
            val src = rememberSource()
            Button(
                onClick = {
                    if (step < 2) step++ else onDone(gender ?: Gender.UNSPECIFIED, picked)
                },
                enabled = canContinue,
                interactionSource = src,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                    .height(64.dp).pressScale(src),
                shape = MaterialTheme.shapes.large,
            ) {
                AnimatedContent(step, label = "cta") { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (s) { 0 -> "Get started"; 1 -> "Continue"; else -> "Start using Laundry Ledger" },
                            fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        )
                        if (s < 2) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDots(step: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val w by androidx.compose.animation.core.animateDpAsState(
                if (i == step) 24.dp else 8.dp, tween(300), label = "dot",
            )
            Box(
                Modifier.height(8.dp).width(w).clip(RoundedCornerShape(50)).background(
                    if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val float = androidx.compose.animation.core.rememberInfiniteTransition(label = "float")
        val dy by float.animateFloat(
            0f, -14f,
            androidx.compose.animation.core.infiniteRepeatable(
                tween(1600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "dy",
        )
        Image(
            painterResource(R.drawable.cloth_basket), null,
            Modifier.size(170.dp).graphicsLayer { translationY = dy * density },
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Never lose a sock\nto the laundry again",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Note down what you hand over, then tick pieces off when they come back. Missing something? You'll know right away.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        PrivacyNote()
    }
}

@Composable
fun PrivacyNote(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(SuccessContainer.copy(alpha = if (androidx.compose.foundation.isSystemInDarkTheme()) 0.12f else 1f))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Success), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Lock, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Your data stays on your phone", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                "No account, no ads, no tracking. Nothing you enter ever leaves this device. Internet is only used to check for app updates.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GenderStep(gender: Gender?, onPick: (Gender) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepTitle(
            "Whose clothes are these?",
            "This only decides which cloth types we suggest. It stays on your phone.",
        )
        Gender.entries.forEach { g ->
            GenderOption(g, gender == g, g.subtitle()) { onPick(g) }
        }
    }
}
