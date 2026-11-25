package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.DiaryEntryEntity
import com.example.data.local.FoodDatabaseSeed
import com.example.data.local.FoodItemEntity
import com.example.domain.calc.NutritionCalculator
import com.example.domain.model.AppLanguage
import com.example.domain.model.FoodCategory
import com.example.domain.model.MealType
import com.example.domain.model.NutrientTotals
import com.example.domain.model.NutritionGoals
import com.example.domain.model.NutritionSourceType
import com.example.i18n.LocalStrings
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    selectedDateIso: String,
    language: AppLanguage,
    goals: NutritionGoals,
    totals: NutrientTotals,
    entries: List<DiaryEntryEntity>,
    searchQuery: String,
    selectedCategory: FoodCategory?,
    favoritesOnly: Boolean,
    foodResults: List<FoodItemEntity>,
    onShiftDate: (Long) -> Unit,
    onSelectToday: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategoryChange: (FoodCategory?) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onToggleFoodFavorite: (FoodItemEntity) -> Unit,
    onLogFood: (FoodItemEntity, MealType, Double, String) -> Unit,
    onAddCustomFood: (String, FoodCategory, Double, Double, Double, Double, Double, Double, Double, MealType) -> Unit,
    onUpdateEntry: (DiaryEntryEntity, Double, MealType) -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var showFoodPickerForMeal by remember { mutableStateOf<MealType?>(null) }
    var selectedFoodForPortion by remember { mutableStateOf<FoodItemEntity?>(null) }
    var showCustomFoodDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<DiaryEntryEntity?>(null) }

    val todayIso = LocalDate.now().toString()
    val yesterdayIso = LocalDate.now().minusDays(1).toString()
    val dateLabel = when (selectedDateIso) {
        todayIso -> "${strings.today} ($selectedDateIso)"
        yesterdayIso -> "${strings.yesterday} ($selectedDateIso)"
        else -> selectedDateIso
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFoodPickerForMeal = MealType.LUNCH },
                modifier = Modifier.testTag("diary_add_fab")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = strings.addFoodToMeal)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(strings.diaryTitle, style = MaterialTheme.typography.headlineMedium)
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onShiftDate(-1) }, modifier = Modifier.testTag("diary_prev_day_btn")) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = strings.back)
                            }
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.clickable { onSelectToday() }
                            )
                            IconButton(onClick = { onShiftDate(1) }, modifier = Modifier.testTag("diary_next_day_btn")) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = strings.next)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(strings.consumedLabel, style = MaterialTheme.typography.labelSmall)
                                Text("${totals.calories.roundToInt()} / ${goals.calories} ${strings.kcal}", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                "P ${totals.proteinG.roundToInt()}${strings.gramsUnit} • " +
                                    "C ${totals.carbsG.roundToInt()}${strings.gramsUnit} • " +
                                    "F ${totals.fatG.roundToInt()}${strings.gramsUnit}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }

                MealType.entries.forEach { meal ->
                    val mealEntries = entries.filter { it.mealType == meal.name }
                    val mealCalories = mealEntries.sumOf { it.calories }.roundToInt()
                    item(key = "header_${meal.name}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${strings.mealName(meal)} ($mealCalories ${strings.kcal})", style = MaterialTheme.typography.titleMedium)
                            TextButton(
                                onClick = { showFoodPickerForMeal = meal },
                                modifier = Modifier.testTag("diary_add_${meal.name.lowercase()}")
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.add)
                            }
                        }
                    }
                    if (mealEntries.isEmpty()) {
                        item(key = "empty_${meal.name}") {
                            Text(
                                text = strings.noDataYet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    } else {
                        items(mealEntries, key = { it.id }) { entry ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.foodName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${entry.servingGrams.roundToInt()} ${strings.gramsUnit} (${entry.portionLabel}) • " +
                                                "${strings.sourceName(NutritionSourceType.fromName(entry.sourceType))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "${entry.calories.roundToInt()} ${strings.kcal} • " +
                                                "P ${entry.proteinG.roundToInt()}g • C ${entry.carbsG.roundToInt()}g • F ${entry.fatG.roundToInt()}g",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { editingEntry = entry }) {
                                        Icon(Icons.Outlined.Edit, contentDescription = strings.edit)
                                    }
                                    IconButton(onClick = { onDeleteEntry(entry.id) }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = strings.delete)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Food Picker Bottom Sheet
    val activeMeal = showFoodPickerForMeal
    if (activeMeal != null) {
        ModalBottomSheet(onDismissRequest = { showFoodPickerForMeal = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${strings.addFoodToMeal} — ${strings.mealName(activeMeal)}", style = MaterialTheme.typography.titleLarge)
                    TextButton(
                        onClick = { showCustomFoodDialog = true },
                        modifier = Modifier.testTag("diary_create_custom_food_btn")
                    ) {
                        Text(strings.createCustomFood)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = strings.search) },
                    placeholder = { Text(strings.searchFoodsHint) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("food_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null && !favoritesOnly,
                        onClick = {
                            onCategoryChange(null)
                            if (favoritesOnly) onToggleFavoritesFilter()
                        },
                        label = { Text(strings.allCategories) }
                    )
                    FilterChip(
                        selected = selectedCategory == FoodCategory.AZERBAIJANI_CUISINE,
                        onClick = {
                            onCategoryChange(
                                if (selectedCategory == FoodCategory.AZERBAIJANI_CUISINE) null
                                else FoodCategory.AZERBAIJANI_CUISINE
                            )
                        },
                        label = { Text(strings.azerbaijaniCuisineFilter) }
                    )
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = onToggleFavoritesFilter,
                        label = { Text(strings.favoritesOnly) }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(foodResults, key = { it.id }) { food ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFoodForPortion = food }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(food.localizedName(language), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${food.caloriesPer100g.roundToInt()} ${strings.kcal}/100${strings.gramsUnit} • ${food.localizedServingLabel(language)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onToggleFoodFavorite(food) }) {
                                    Icon(
                                        imageVector = if (food.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = strings.favoritesOnly,
                                        tint = if (food.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Portion Selection Dialog
    val foodToPortion = selectedFoodForPortion
    if (foodToPortion != null && activeMeal != null) {
        var gramsText by remember(foodToPortion.id) { mutableStateOf(foodToPortion.defaultServingGrams.roundToInt().toString()) }
        var portionLabel by remember(foodToPortion.id) { mutableStateOf(foodToPortion.localizedServingLabel(language)) }
        val grams = gramsText.replace(',', '.').toDoubleOrNull()?.coerceIn(1.0, 2000.0) ?: foodToPortion.defaultServingGrams
        val measures = remember(foodToPortion.householdMeasuresJson) { FoodDatabaseSeed.parseMeasures(foodToPortion.householdMeasuresJson) }

        AlertDialog(
            onDismissRequest = { selectedFoodForPortion = null },
            title = { Text(foodToPortion.localizedName(language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (measures.isNotEmpty()) {
                        Text(strings.householdMeasuresLabel, style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            measures.forEach { m ->
                                FilterChip(
                                    selected = kotlin.math.abs(grams - m.grams) < 1.0,
                                    onClick = {
                                        gramsText = m.grams.roundToInt().toString()
                                        portionLabel = m.localizedLabel(language)
                                    },
                                    label = { Text("${m.localizedLabel(language)} (${m.grams.roundToInt()}g)") }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = gramsText,
                        onValueChange = {
                            gramsText = it
                            portionLabel = "${it} ${strings.gramsUnit}"
                        },
                        label = { Text(strings.portionGramsLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val estKcal = NutritionCalculator.scalePer100g(foodToPortion.caloriesPer100g, grams).roundToInt()
                    val estP = NutritionCalculator.scalePer100g(foodToPortion.proteinPer100g, grams).roundToInt()
                    val estC = NutritionCalculator.scalePer100g(foodToPortion.carbsPer100g, grams).roundToInt()
                    val estF = NutritionCalculator.scalePer100g(foodToPortion.fatPer100g, grams).roundToInt()
                    Text(
                        "$estKcal ${strings.kcal} • P ${estP}g • C ${estC}g • F ${estF}g",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLogFood(foodToPortion, activeMeal, grams, portionLabel)
                        selectedFoodForPortion = null
                        showFoodPickerForMeal = null
                    },
                    modifier = Modifier.testTag("confirm_add_food_btn")
                ) {
                    Text(strings.add)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFoodForPortion = null }) { Text(strings.cancel) }
            }
        )
    }

    // Custom Food Dialog
    if (showCustomFoodDialog && activeMeal != null) {
        var cName by remember { mutableStateOf("") }
        var cKcal by remember { mutableStateOf("180") }
        var cProtein by remember { mutableStateOf("8") }
        var cCarbs by remember { mutableStateOf("20") }
        var cFat by remember { mutableStateOf("7") }
        var cFiber by remember { mutableStateOf("2") }
        var cSugar by remember { mutableStateOf("3") }
        var cGrams by remember { mutableStateOf("150") }

        AlertDialog(
            onDismissRequest = { showCustomFoodDialog = false },
            title = { Text(strings.createCustomFood) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = cName, onValueChange = { cName = it }, label = { Text(strings.foodNameLabel) }, singleLine = true)
                    OutlinedTextField(value = cKcal, onValueChange = { cKcal = it }, label = { Text(strings.caloriesPer100gLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cProtein, onValueChange = { cProtein = it }, label = { Text(strings.proteinPer100gLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = cCarbs, onValueChange = { cCarbs = it }, label = { Text(strings.carbsPer100gLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cFat, onValueChange = { cFat = it }, label = { Text(strings.fatPer100gLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = cFiber, onValueChange = { cFiber = it }, label = { Text(strings.fiberPer100gLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cSugar, onValueChange = { cSugar = it }, label = { Text(strings.sugarPer100gLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = cGrams, onValueChange = { cGrams = it }, label = { Text(strings.portionGramsLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = cName.isNotBlank(),
                    onClick = {
                        onAddCustomFood(
                            cName.trim(),
                            FoodCategory.OTHER,
                            cKcal.toDoubleOrNull() ?: 0.0,
                            cProtein.toDoubleOrNull() ?: 0.0,
                            cCarbs.toDoubleOrNull() ?: 0.0,
                            cFat.toDoubleOrNull() ?: 0.0,
                            cFiber.toDoubleOrNull() ?: 0.0,
                            cSugar.toDoubleOrNull() ?: 0.0,
                            cGrams.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0,
                            activeMeal
                        )
                        showCustomFoodDialog = false
                        showFoodPickerForMeal = null
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomFoodDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Edit Diary Entry Dialog
    val entryToEdit = editingEntry
    if (entryToEdit != null) {
        var newGramsText by remember(entryToEdit.id) { mutableStateOf(entryToEdit.servingGrams.roundToInt().toString()) }
        var newMeal by remember(entryToEdit.id) { mutableStateOf(MealType.fromName(entryToEdit.mealType)) }

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text(entryToEdit.foodName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newGramsText,
                        onValueChange = { newGramsText = it },
                        label = { Text(strings.portionGramsLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Text(strings.selectMealCategory, style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MealType.entries.forEach { m ->
                            FilterChip(
                                selected = newMeal == m,
                                onClick = { newMeal = m },
                                label = { Text(strings.mealName(m)) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val g = newGramsText.replace(',', '.').toDoubleOrNull()?.coerceIn(1.0, 3000.0) ?: entryToEdit.servingGrams
                    onUpdateEntry(entryToEdit, g, newMeal)
                    editingEntry = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingEntry = null }) { Text(strings.cancel) }
            }
        )
    }
}
