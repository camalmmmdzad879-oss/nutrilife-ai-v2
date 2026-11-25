package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DiaryEntryEntity
import com.example.domain.model.MealType
import com.example.domain.model.NutrientTotals
import com.example.domain.model.NutritionGoals
import com.example.domain.model.UserProfile
import com.example.i18n.LocalStrings
import com.example.ui.components.CalorieProgressRing
import com.example.ui.components.InfoNoticeBanner
import com.example.ui.components.MacroProgressBar
import com.example.ui.theme.MacroCarbsColor
import com.example.ui.theme.MacroFatColor
import com.example.ui.theme.MacroFiberColor
import com.example.ui.theme.MacroProteinColor
import com.example.ui.theme.WaterSkyColor
import com.example.ui.viewmodel.UiAsyncState
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    profile: UserProfile,
    goals: NutritionGoals,
    totals: NutrientTotals,
    waterTodayMl: Int,
    diaryEntries: List<DiaryEntryEntity>,
    showUpdatedBanner: Boolean,
    onDismissUpdatedBanner: () -> Unit,
    onQuickAddWater: (Int) -> Unit,
    onOpenScannerPhoto: () -> Unit,
    onOpenScannerBarcode: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenRecipes: () -> Unit,
    onOpenSettings: () -> Unit,
    insightState: UiAsyncState<String> = UiAsyncState.Idle,
    isLiveGeminiEnabled: Boolean = true,
    onRequestDailyInsight: () -> Unit = {}
) {
    val strings = LocalStrings.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("home_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val greeting = if (profile.name.isNotBlank()) "${strings.homeGreeting}, ${profile.name}" else strings.homeGreeting
                        Text(greeting, style = MaterialTheme.typography.headlineMedium)
                        Text(strings.appName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = strings.navSettings)
                    }
                }
            }

            item {
                AnimatedVisibility(visible = showUpdatedBanner) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.updateRecognizedBanner,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismissUpdatedBanner) {
                                Icon(Icons.Outlined.Close, contentDescription = strings.close)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_nutrition),
                            contentDescription = strings.appName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(116.dp),
                            contentScale = ContentScale.Crop
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CalorieProgressRing(
                                consumed = totals.calories.roundToInt(),
                                target = goals.calories
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MacroProgressBar(strings.proteinLabel, totals.proteinG, goals.proteinG, MacroProteinColor)
                                MacroProgressBar(strings.carbsLabel, totals.carbsG, goals.carbsG, MacroCarbsColor)
                                MacroProgressBar(strings.fatLabel, totals.fatG, goals.fatG, MacroFatColor)
                                MacroProgressBar(strings.fiberLabel, totals.fiberG, goals.fiberG, MacroFiberColor)
                            }
                        }
                    }
                }
            }

            item {
                Text(strings.quickActionsTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenScannerPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_scan_photo_btn")
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.scanMealPhotoCta, maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = onOpenScannerBarcode,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_scan_barcode_btn")
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.scanBarcodeCta, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenPlanner,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_planner_btn")
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.mealPlannerCta, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenRecipes,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_recipes_btn")
                    ) {
                        Icon(Icons.Outlined.RestaurantMenu, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.recipeCalcCta, maxLines = 1)
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_ai_insight_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = strings.aiInsightTitle,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = strings.aiInsightTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            if (isLiveGeminiEnabled) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = strings.aiLiveBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        when (insightState) {
                            UiAsyncState.Idle -> {
                                Text(
                                    text = strings.aiInsightPromptHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            UiAsyncState.Loading -> {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text(strings.loading, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            is UiAsyncState.Success -> {
                                Text(
                                    text = insightState.data,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            is UiAsyncState.Error -> {
                                Text(
                                    text = insightState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("home_ai_insight_error")
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = onRequestDailyInsight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_ai_insight_btn")
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (insightState is UiAsyncState.Error) strings.retry else strings.aiInsightCta)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.WaterDrop, contentDescription = strings.waterIntakeTitle, tint = WaterSkyColor)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.waterIntakeTitle, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                "$waterTodayMl / ${goals.waterMl} ${strings.mlUnit}",
                                style = MaterialTheme.typography.labelLarge,
                                color = WaterSkyColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (waterTodayMl.toFloat() / goals.waterMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.extraSmall),
                            color = WaterSkyColor
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(200, 250, 500).forEach { ml ->
                                FilledTonalButton(
                                    onClick = { onQuickAddWater(ml) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_water_add_$ml"),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("$ml ${strings.mlUnit}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.todaysMealsTitle, style = MaterialTheme.typography.titleMedium)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .testTag("home_open_diary_chip")
                            .clickable { onOpenDiary() }
                    ) {
                        Text(
                            text = strings.addFoodToMeal,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { meal ->
                        val mealItems = diaryEntries.filter { it.mealType == meal.name }
                        val mealKcal = mealItems.sumOf { it.calories }.roundToInt()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDiary() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(strings.mealName(meal), style = MaterialTheme.typography.titleSmall)
                                    val summary = if (mealItems.isEmpty()) strings.noDataYet else mealItems.joinToString(", ") { it.foodName }
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "$mealKcal ${strings.kcal}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                InfoNoticeBanner(
                    icon = Icons.Outlined.Info,
                    body = strings.aiEstimationBanner + " " + strings.medicalDisclaimerShort
                )
            }
        }
    }
}
