package dev.mrbean.laundryledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.mrbean.laundryledger.ui.BatchScreen
import dev.mrbean.laundryledger.ui.ClothTypesScreen
import dev.mrbean.laundryledger.ui.OnboardingScreen
import dev.mrbean.laundryledger.ui.SettingsScreen
import dev.mrbean.laundryledger.ui.HomeScreen
import dev.mrbean.laundryledger.ui.LaundryTheme
import dev.mrbean.laundryledger.ui.LedgerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LaundryTheme {
              androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) {
                val nav = rememberNavController()
                val vm: LedgerViewModel = viewModel()
                val dur = 380
                val ease = FastOutSlowInEasing
                NavHost(
                    navController = nav,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    enterTransition = {
                        slideInHorizontally(tween(dur, easing = ease)) { it / 3 } + fadeIn(tween(dur))
                    },
                    exitTransition = {
                        slideOutHorizontally(tween(dur, easing = ease)) { -it / 6 } +
                            fadeOut(tween(dur / 2)) + scaleOut(tween(dur), targetScale = 0.96f)
                    },
                    popEnterTransition = {
                        slideInHorizontally(tween(dur, easing = ease)) { -it / 6 } +
                            fadeIn(tween(dur)) + scaleIn(tween(dur), initialScale = 0.96f)
                    },
                    popExitTransition = {
                        slideOutHorizontally(tween(dur, easing = ease)) { it / 3 } + fadeOut(tween(dur / 2))
                    },
                ) {
                    composable("home") {
                        val onboarded by vm.prefs.onboarded.collectAsState()
                        if (!onboarded) {
                            OnboardingScreen { g, types -> vm.prefs.completeOnboarding(g, types) }
                        } else {
                            HomeScreen(
                                vm = vm,
                                onOpenBatch = { id -> nav.navigate("batch/$id") },
                                onOpenSettings = { nav.navigate("settings") },
                            )
                        }
                    }
                    composable("settings") {
                        SettingsScreen(vm, onBack = { nav.popBackStack() }, onEditClothes = { nav.navigate("clothes") })
                    }
                    composable("clothes") {
                        ClothTypesScreen(vm, onBack = { nav.popBackStack() })
                    }
                    composable(
                        "batch/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments!!.getLong("id")
                        BatchScreen(vm = vm, batchId = id, onBack = {
                            vm.discardIfEmpty(id)
                            nav.popBackStack()
                        })
                    }
                }
              }
            }
        }
    }
}
