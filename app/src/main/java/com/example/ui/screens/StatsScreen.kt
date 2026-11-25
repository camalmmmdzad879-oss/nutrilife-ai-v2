package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.local.DiaryEntryEntity
import com.example.data.local.ExerciseLogEntity
import com.example.data.local.WaterLogEntity
import com.example.data.local.WeightLogEntity
import com.example.domain.calc.NutritionCalculator
import com.example.domain.model.AppLanguage
import com.example.domain.model.ExerciseType
import com.example.domain.model.NutrientTotals
import com.example.domain.model.NutritionGoals
import com.example.domain.model.UserProfile
import com.example.i18n.LocalStrings
import com.example.ui.components.InfoNoticeBanner
import com.example.ui.components.MetricPill
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.SimpleLineChart
import com.example.ui.theme.WaterSkyColor
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    language: AppLanguage,
    profile: UserProfile,
    goals: NutritionGoals,
    dailyTotals: NutrientTotals,
    diaryLast30Days: List<DiaryEntryEntity>,
    waterTodayLogs: List<WaterLogEntity>,
    waterLast7Days: List<WaterLogEntity>,
    weightHistory: List<WeightLogEntity>,
    exerciseTodayLogs: List<ExerciseLogEntity>,
    exerciseLast30Days: List<ExerciseLogEntity>,
    onAddWater: (Int) -> Unit,
    onDeleteWater: (Long) -> Unit,
    onUpdateWaterTarget: (Int) -> Unit,
    onAddWeight: (Double, String) -> Unit,
    onUpdateWeight: (WeightLogEntity) -> Unit,
    onDeleteWeight: (Long) -> Unit,
    onAddExercise: (ExerciseType, Int, Int?, String) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onExportCsv: () -> Unit
) {
    val strings = LocalStrings.current
    var activeTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        strings.tabOverview,
        strings.tabMicronutrients,
        strings.tabWater,
        strings.tabWeight,
        strings.tabExercise,
        strings.tabReports
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            Text(
                text = strings.statsTitle,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            ScrollableTabRow(selectedTabIndex = activeTab, edgePadding = 12.dp) {
                tabTitles.forEachIndexed { idx, title ->
                    Tab(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        text = { Text(title) },
                        modifier = Modifier.testTag("stats_tab_$idx")
                    )
                }
            }

            when (activeTab) {
                0 -> OverviewStatsTab(goals, diaryLast30Days)
                1 -> MicronutrientsTab(dailyTotals, goals)
                2 -> WaterTab(goals, waterTodayLogs, waterLast7Days, onAddWater, onDeleteWater, onUpdateWaterTarget)
                3 -> WeightTab(profile, weightHistory, onAddWeight, onUpdateWeight, onDeleteWeight)
                4 -> ExerciseTab(language, profile, exerciseTodayLogs, onAddExercise, onDeleteExercise)
                5 -> ReportsTab(diaryLast30Days, waterLast7Days, weightHistory, exerciseLast30Days, onExportCsv)
            }
        }
    }
}

@Composable
private fun OverviewStatsTab(
    goals: NutritionGoals,
    diaryLast30Days: List<DiaryEntryEntity>
) {
    val strings = LocalStrings.current
    val today = LocalDate.now()
    val last7Dates = (6L downTo 0L).map { today.minusDays(it) }
    val chartData = last7Dates.map { date ->
        val dayTotal = diaryLast30Days.filter { it.dateIso == date.toString() }.sumOf { it.calories }.toFloat()
        date.dayOfMonth.toString() to dayTotal
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.weeklyCaloriesTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    SimpleBarChart(values = chartData, targetLine = goals.calories.toFloat())
                }
            }
        }
        item {
            val activeDays = chartData.count { it.second > 0f }.coerceAtLeast(1)
            val avgCalories = (chartData.sumOf { it.second.toDouble() } / activeDays).roundToInt()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill(strings.weeklySummaryTitle, "$avgCalories ${strings.kcal}/d", modifier = Modifier.weight(1f))
                MetricPill(strings.targetLabel, "${goals.calories} ${strings.kcal}", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MicronutrientsTab(
    totals: NutrientTotals,
    goals: NutritionGoals
) {
    val strings = LocalStrings.current
    val rows = listOf(
        Triple(strings.fiberLabel, "${totals.fiberG.roundToInt()} ${strings.gramsUnit}", "${goals.fiberG} ${strings.gramsUnit}"),
        Triple(strings.sugarLabel, "${totals.sugarG.roundToInt()} ${strings.gramsUnit}", "< ${goals.sugarLimitG} ${strings.gramsUnit}"),
        Triple(strings.sodiumLabel, "${totals.sodiumMg.roundToInt()} mg", "< ${goals.sodiumLimitMg} mg"),
        Triple(strings.calciumLabel, "${totals.calciumMg.roundToInt()} mg", "1000 mg"),
        Triple(strings.ironLabel, String.format("%.1f mg", totals.ironMg), "8–18 mg"),
        Triple(strings.potassiumLabel, "${totals.potassiumMg.roundToInt()} mg", "3400 mg"),
        Triple(strings.magnesiumLabel, "${totals.magnesiumMg.roundToInt()} mg", "320–400 mg"),
        Triple(strings.vitaminALabel, "${totals.vitaminAMcg.roundToInt()} mcg", "700–900 mcg"),
        Triple(strings.vitaminB12Label, String.format("%.1f mcg", totals.vitaminB12Mcg), "2.4 mcg"),
        Triple(strings.vitaminCLabel, "${totals.vitaminCMg.roundToInt()} mg", "75–90 mg"),
        Triple(strings.vitaminDLabel, String.format("%.1f mcg", totals.vitaminDMcg), "15 mcg")
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            InfoNoticeBanner(icon = Icons.Outlined.Info, body = strings.micronutrientsDisclaimer)
        }
        items(rows) { (name, current, ref) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    Text("$current / $ref", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun WaterTab(
    goals: NutritionGoals,
    todayLogs: List<WaterLogEntity>,
    waterLast7Days: List<WaterLogEntity>,
    onAddWater: (Int) -> Unit,
    onDeleteWater: (Long) -> Unit,
    onUpdateWaterTarget: (Int) -> Unit
) {
    val strings = LocalStrings.current
    var customWaterText by remember { mutableStateOf("300") }
    var targetText by remember(goals.waterMl) { mutableStateOf(goals.waterMl.toString()) }
    val todayTotal = todayLogs.sumOf { it.amountMl }
    val today = LocalDate.now()
    val chartData = (6L downTo 0L).map { d ->
        val date = today.minusDays(d)
        val sum = waterLast7Days.filter { it.dateIso == date.toString() }.sumOf { it.amountMl }.toFloat()
        date.dayOfMonth.toString() to sum
    }

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
                    Text("${strings.waterIntakeTitle}: $todayTotal / ${goals.waterMl} ${strings.mlUnit}", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customWaterText,
                            onValueChange = { customWaterText = it.filter { c -> c.isDigit() } },
                            label = { Text(strings.addWaterCustomLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            val ml = customWaterText.toIntOrNull() ?: 0
                            if (ml > 0) onAddWater(ml)
                        }) { Text(strings.add) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                            label = { Text(strings.adjustWaterTargetLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            val t = targetText.toIntOrNull()
                            if (t != null) onUpdateWaterTarget(t)
                        }) { Text(strings.save) }
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strings.waterHistoryTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    SimpleBarChart(values = chartData, targetLine = goals.waterMl.toFloat(), barColor = WaterSkyColor)
                }
            }
        }
        items(todayLogs, key = { it.id }) { log ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${log.amountMl} ${strings.mlUnit}", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { onDeleteWater(log.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = strings.delete)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightTab(
    profile: UserProfile,
    weightHistory: List<WeightLogEntity>,
    onAddWeight: (Double, String) -> Unit,
    onUpdateWeight: (WeightLogEntity) -> Unit,
    onDeleteWeight: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var weightText by remember { mutableStateOf(profile.currentWeightKg.toString()) }
    var editingLog by remember { mutableStateOf<WeightLogEntity?>(null) }

    val chronological = weightHistory.reversed()
    val chartPoints = chronological.map { it.dateIso.takeLast(5) to it.weightKg.toFloat() }
    val weeklyDelta = if (chronological.size >= 2) {
        chronological.last().weightKg - chronological.first().weightKg
    } else 0.0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill(strings.currentWeightLabel, "${profile.currentWeightKg} ${strings.kgUnit}", modifier = Modifier.weight(1f))
                MetricPill(strings.goalWeightLabel, "${profile.goalWeightKg} ${strings.kgUnit}", modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.logWeightTitle, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text(strings.weightKgInputLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("weight_input")
                        )
                        Button(
                            onClick = {
                                val w = weightText.replace(',', '.').toDoubleOrNull()
                                if (w != null && w in 25.0..300.0) onAddWeight(w, "")
                            },
                            modifier = Modifier.testTag("weight_save_btn")
                        ) { Text(strings.save) }
                    }
                }
            }
        }
        if (chartPoints.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${strings.weightHistoryTitle} (${strings.weightDeltaWeekly}: ${String.format("%+.1f", weeklyDelta)} ${strings.kgUnit})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        SimpleLineChart(points = chartPoints)
                    }
                }
            }
        }
        items(weightHistory, key = { it.id }) { log ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${log.weightKg} ${strings.kgUnit}", style = MaterialTheme.typography.titleSmall)
                        Text(log.dateIso, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { editingLog = log }) { Icon(Icons.Outlined.Edit, contentDescription = strings.edit) }
                    IconButton(onClick = { onDeleteWeight(log.id) }) { Icon(Icons.Outlined.Delete, contentDescription = strings.delete) }
                }
            }
        }
    }

    val toEdit = editingLog
    if (toEdit != null) {
        var newWeight by remember(toEdit.id) { mutableStateOf(toEdit.weightKg.toString()) }
        AlertDialog(
            onDismissRequest = { editingLog = null },
            title = { Text(strings.edit) },
            text = {
                OutlinedTextField(
                    value = newWeight,
                    onValueChange = { newWeight = it },
                    label = { Text(strings.weightKgInputLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val w = newWeight.replace(',', '.').toDoubleOrNull()
                    if (w != null && w in 25.0..300.0) {
                        onUpdateWeight(toEdit.copy(weightKg = w))
                    }
                    editingLog = null
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { editingLog = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun ExerciseTab(
    language: AppLanguage,
    profile: UserProfile,
    todayLogs: List<ExerciseLogEntity>,
    onAddExercise: (ExerciseType, Int, Int?, String) -> Unit,
    onDeleteExercise: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var selectedType by remember { mutableStateOf(ExerciseType.WALKING) }
    var durationText by remember { mutableStateOf("30") }
    var manualKcalText by remember { mutableStateOf("") }

    val duration = durationText.toIntOrNull()?.coerceIn(1, 600) ?: 30
    val autoEstimate = NutritionCalculator.estimateExerciseCalories(selectedType, duration, profile.currentWeightKg)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoNoticeBanner(
                icon = Icons.Outlined.Info,
                body = "${strings.exerciseEstimatedBurnNote} ${strings.exerciseNotSubtractedNote}"
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.logExerciseTitle, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExerciseType.entries.forEach { t ->
                            FilterChip(
                                selected = selectedType == t,
                                onClick = { selectedType = t },
                                label = { Text(strings.exerciseName(t, language)) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { durationText = it.filter { c -> c.isDigit() } },
                            label = { Text(strings.exerciseDurationLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualKcalText,
                            onValueChange = { manualKcalText = it.filter { c -> c.isDigit() } },
                            label = { Text("~ $autoEstimate ${strings.kcal}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = { onAddExercise(selectedType, duration, manualKcalText.toIntOrNull(), "") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(strings.add) }
                }
            }
        }
        items(todayLogs, key = { it.id }) { ex ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.exerciseName(ExerciseType.fromName(ex.activityType), language), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${ex.durationMinutes} ${strings.minUnit} • ~${ex.estimatedCaloriesBurned} ${strings.kcal} (${strings.sourceAiEstimated})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteExercise(ex.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = strings.delete)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsTab(
    diaryLast30Days: List<DiaryEntryEntity>,
    waterLast7Days: List<WaterLogEntity>,
    weightHistory: List<WeightLogEntity>,
    exerciseLast30Days: List<ExerciseLogEntity>,
    onExportCsv: () -> Unit
) {
    val strings = LocalStrings.current
    val weekCutoff = LocalDate.now().minusDays(7).toString()
    val weeklyEntries = diaryLast30Days.filter { it.dateIso >= weekCutoff }
    val weeklyAvgKcal = weeklyEntries.groupBy { it.dateIso }.values
        .map { day -> day.sumOf { it.calories } }
        .takeIf { it.isNotEmpty() }?.average()?.roundToInt() ?: 0
    val monthlyAvgKcal = diaryLast30Days.groupBy { it.dateIso }.values
        .map { day -> day.sumOf { it.calories } }
        .takeIf { it.isNotEmpty() }?.average()?.roundToInt() ?: 0
    val weeklyWaterAvg = waterLast7Days.groupBy { it.dateIso }.values
        .map { day -> day.sumOf { it.amountMl } }
        .takeIf { it.isNotEmpty() }?.average()?.roundToInt() ?: 0
    val monthlyExerciseMin = exerciseLast30Days.sumOf { it.durationMinutes }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.weeklySummaryTitle, style = MaterialTheme.typography.titleMedium)
                    Text("${strings.consumedLabel}: ~$weeklyAvgKcal ${strings.kcal}/day", style = MaterialTheme.typography.bodyMedium)
                    Text("${strings.waterIntakeTitle}: ~$weeklyWaterAvg ${strings.mlUnit}/day", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.monthlySummaryTitle, style = MaterialTheme.typography.titleMedium)
                    Text("${strings.consumedLabel}: ~$monthlyAvgKcal ${strings.kcal}/day", style = MaterialTheme.typography.bodyMedium)
                    Text("${strings.tabWeight}: ${weightHistory.size} records", style = MaterialTheme.typography.bodyMedium)
                    Text("${strings.tabExercise}: $monthlyExerciseMin ${strings.minUnit}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Button(
                onClick = onExportCsv,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_csv_btn")
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null)
                Spacer(Modifier.widthIn(8.dp))
                Text(strings.exportCsvBtn)
            }
        }
    }
}
