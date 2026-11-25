package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.FoodItemEntity
import com.example.data.local.RecipeEntity
import com.example.data.local.SavedMealPlanEntity
import com.example.domain.calc.NutritionCalculator
import com.example.domain.model.AppLanguage
import com.example.domain.model.GeneratedMealPlan
import com.example.domain.model.MealType
import com.example.i18n.LocalStrings
import com.example.ui.components.InfoNoticeBanner
import com.example.ui.viewmodel.RecipeIngredientDraft
import com.example.ui.viewmodel.UiAsyncState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerAndRecipesScreen(
    initialTab: Int,
    language: AppLanguage,
    isLiveGeminiEnabled: Boolean,
    mealPlanState: UiAsyncState<GeneratedMealPlan>,
    savedPlans: List<SavedMealPlanEntity>,
    foodResults: List<FoodItemEntity>,
    searchQuery: String,
    recipeIngredients: List<RecipeIngredientDraft>,
    savedRecipes: List<RecipeEntity>,
    onBack: () -> Unit,
    onGeneratePlan: () -> Unit,
    onReplaceMeal: (MealType) -> Unit,
    onSavePlan: () -> Unit,
    onDeletePlan: (Long) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddRecipeIngredient: (FoodItemEntity, Double) -> Unit,
    onRemoveRecipeIngredient: (Int) -> Unit,
    onSaveRecipe: (String, Int) -> Unit,
    onLogRecipeServing: (RecipeEntity, MealType) -> Unit,
    onDeleteRecipe: (Long) -> Unit
) {
    BackHandler(onBack = onBack)
    val strings = LocalStrings.current
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.plannerTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("planner_back_btn")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text(strings.tabMealPlanner) },
                        modifier = Modifier.testTag("planner_tab_plan")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text(strings.tabRecipeCalculator) },
                        modifier = Modifier.testTag("planner_tab_recipe")
                    )
                }

                if (activeTab == 0) {
                    MealPlannerTab(
                        isLiveGeminiEnabled = isLiveGeminiEnabled,
                        mealPlanState = mealPlanState,
                        savedPlans = savedPlans,
                        onGeneratePlan = onGeneratePlan,
                        onReplaceMeal = onReplaceMeal,
                        onSavePlan = onSavePlan,
                        onDeletePlan = onDeletePlan
                    )
                } else {
                    RecipeCalculatorTab(
                        language = language,
                        foodResults = foodResults,
                        searchQuery = searchQuery,
                        ingredients = recipeIngredients,
                        savedRecipes = savedRecipes,
                        onSearchQueryChange = onSearchQueryChange,
                        onAddIngredient = onAddRecipeIngredient,
                        onRemoveIngredient = onRemoveRecipeIngredient,
                        onSaveRecipe = onSaveRecipe,
                        onLogServing = onLogRecipeServing,
                        onDeleteRecipe = onDeleteRecipe
                    )
                }
            }
        }
    }
}

@Composable
private fun MealPlannerTab(
    isLiveGeminiEnabled: Boolean,
    mealPlanState: UiAsyncState<GeneratedMealPlan>,
    savedPlans: List<SavedMealPlanEntity>,
    onGeneratePlan: () -> Unit,
    onReplaceMeal: (MealType) -> Unit,
    onSavePlan: () -> Unit,
    onDeletePlan: (Long) -> Unit
) {
    val strings = LocalStrings.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isLiveGeminiEnabled) {
            item {
                InfoNoticeBanner(
                    icon = Icons.Outlined.Info,
                    title = strings.aiServiceStatusTitle,
                    body = strings.mealPlanPreviewNotice,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        item {
            Button(
                onClick = onGeneratePlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_meal_plan_btn")
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.generateMealPlanBtn)
            }
        }

        when (mealPlanState) {
            UiAsyncState.Idle -> Unit
            UiAsyncState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiAsyncState.Error -> {
                item { InfoNoticeBanner(icon = Icons.Outlined.Info, body = mealPlanState.message) }
            }
            is UiAsyncState.Success -> {
                val plan = mealPlanState.data
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(plan.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${plan.totalCalories} ${strings.kcal} • P ${plan.totalProtein.roundToInt()}g • C ${plan.totalCarbs.roundToInt()}g • F ${plan.totalFat.roundToInt()}g",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(plan.note, style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(
                                onClick = onSavePlan,
                                modifier = Modifier.testTag("save_meal_plan_btn")
                            ) {
                                Icon(Icons.Outlined.BookmarkAdded, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.saveFavoritePlanBtn)
                            }
                        }
                    }
                }
                items(plan.meals, key = { it.mealType.name }) { meal ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.mealName(meal.mealType), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(meal.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${meal.portionGrams.roundToInt()} ${strings.gramsUnit} • ${meal.calories} ${strings.kcal} • " +
                                        "P ${meal.proteinG.roundToInt()}g • C ${meal.carbsG.roundToInt()}g • F ${meal.fatG.roundToInt()}g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onReplaceMeal(meal.mealType) }) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(strings.replaceMealBtn)
                            }
                        }
                    }
                }
            }
        }

        if (savedPlans.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                Text(strings.savedPlansTitle, style = MaterialTheme.typography.titleMedium)
            }
            items(savedPlans, key = { it.id }) { saved ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(saved.title, style = MaterialTheme.typography.titleSmall)
                            Text("${saved.totalCalories} ${strings.kcal}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDeletePlan(saved.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = strings.delete)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeCalculatorTab(
    language: AppLanguage,
    foodResults: List<FoodItemEntity>,
    searchQuery: String,
    ingredients: List<RecipeIngredientDraft>,
    savedRecipes: List<RecipeEntity>,
    onSearchQueryChange: (String) -> Unit,
    onAddIngredient: (FoodItemEntity, Double) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onSaveRecipe: (String, Int) -> Unit,
    onLogServing: (RecipeEntity, MealType) -> Unit,
    onDeleteRecipe: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var recipeName by remember { mutableStateOf("") }
    var servingsText by remember { mutableStateOf("4") }
    var showIngredientPicker by remember { mutableStateOf(false) }
    var ingredientGramsText by remember { mutableStateOf("100") }

    val servings = servingsText.toIntOrNull()?.coerceIn(1, 50) ?: 1
    val totalKcal = ingredients.sumOf { NutritionCalculator.scalePer100g(it.food.caloriesPer100g, it.grams) }
    val totalP = ingredients.sumOf { NutritionCalculator.scalePer100g(it.food.proteinPer100g, it.grams) }
    val totalC = ingredients.sumOf { NutritionCalculator.scalePer100g(it.food.carbsPer100g, it.grams) }
    val totalF = ingredients.sumOf { NutritionCalculator.scalePer100g(it.food.fatPer100g, it.grams) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = recipeName,
                            onValueChange = { recipeName = it },
                            label = { Text(strings.recipeNameLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = servingsText,
                            onValueChange = { servingsText = it.filter { c -> c.isDigit() } },
                            label = { Text(strings.servingsCountLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(
                        onClick = { showIngredientPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.addIngredientBtn)
                    }

                    ingredients.forEachIndexed { idx, ing ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${ing.food.localizedName(language)} — ${ing.grams.roundToInt()} ${strings.gramsUnit}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveIngredient(idx) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = strings.delete)
                            }
                        }
                    }

                    if (ingredients.isNotEmpty()) {
                        Text(
                            "${strings.totalNutritionLabel}: ${totalKcal.roundToInt()} ${strings.kcal} (P ${totalP.roundToInt()}g • C ${totalC.roundToInt()}g • F ${totalF.roundToInt()}g)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "${strings.perServingNutritionLabel} (1/$servings): ${(totalKcal / servings).roundToInt()} ${strings.kcal} " +
                                "(P ${(totalP / servings).roundToInt()}g • C ${(totalC / servings).roundToInt()}g • F ${(totalF / servings).roundToInt()}g)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            enabled = recipeName.isNotBlank(),
                            onClick = {
                                onSaveRecipe(recipeName, servings)
                                recipeName = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(strings.saveRecipeBtn) }
                    }
                }
            }
        }

        if (savedRecipes.isNotEmpty()) {
            item { Text(strings.savedRecipesTitle, style = MaterialTheme.typography.titleMedium) }
            items(savedRecipes, key = { it.id }) { rec ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rec.name, style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { onDeleteRecipe(rec.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = strings.delete)
                            }
                        }
                        Text(rec.ingredientsSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${strings.perServingNutritionLabel}: ${(rec.totalCalories / rec.servings).roundToInt()} ${strings.kcal}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MealType.entries.forEach { meal ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onLogServing(rec, meal) },
                                    label = { Text("+ ${strings.mealName(meal)}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIngredientPicker) {
        ModalBottomSheet(onDismissRequest = { showIngredientPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ingredientGramsText,
                    onValueChange = { ingredientGramsText = it },
                    label = { Text(strings.portionGramsLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text(strings.searchFoodsHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(foodResults, key = { it.id }) { food ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val g = ingredientGramsText.replace(',', '.').toDoubleOrNull()?.coerceIn(1.0, 2000.0) ?: 100.0
                                    onAddIngredient(food, g)
                                    showIngredientPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(food.localizedName(language), style = MaterialTheme.typography.bodyMedium)
                                Text("${food.caloriesPer100g.roundToInt()} ${strings.kcal}/100g", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
