package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.calc.NutritionCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.AppLanguage
import com.example.domain.model.BiologicalSex
import com.example.domain.model.NutritionGoal
import com.example.domain.model.UserProfile
import com.example.i18n.LocalStrings
import com.example.ui.components.InfoNoticeBanner
import com.example.ui.components.LanguageSelectorRow

@Composable
fun OnboardingScreen(
    initialProfile: UserProfile,
    onLanguageSelected: (AppLanguage) -> Unit,
    onComplete: (UserProfile) -> Unit
) {
    val strings = LocalStrings.current
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(initialProfile.name) }
    var ageText by remember { mutableStateOf(initialProfile.age.toString()) }
    var heightText by remember { mutableStateOf(initialProfile.heightCm.toString()) }
    var currentWeightText by remember { mutableStateOf(initialProfile.currentWeightKg.toString()) }
    var goalWeightText by remember { mutableStateOf(initialProfile.goalWeightKg.toString()) }
    var sex by remember { mutableStateOf(initialProfile.sex) }
    var activity by remember { mutableStateOf(initialProfile.activityLevel) }
    var goal by remember { mutableStateOf(initialProfile.nutritionGoal) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val parsedAge = ageText.toIntOrNull() ?: 0
    val parsedHeight = heightText.toIntOrNull() ?: 0
    val parsedCurrentWeight = currentWeightText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val parsedGoalWeight = goalWeightText.replace(',', '.').toDoubleOrNull() ?: parsedCurrentWeight

    val draftProfile = UserProfile(
        name = name.trim(),
        age = parsedAge.coerceIn(10, 110),
        heightCm = parsedHeight.coerceIn(100, 240),
        currentWeightKg = parsedCurrentWeight.coerceIn(25.0, 300.0),
        goalWeightKg = parsedGoalWeight.coerceIn(25.0, 300.0),
        sex = sex,
        activityLevel = activity,
        nutritionGoal = goal,
        language = initialProfile.language,
        dailyWaterTargetMl = NutritionCalculator.recommendedWaterMl(parsedCurrentWeight.coerceIn(30.0, 250.0)),
        onboardingCompleted = true
    )
    val calculatedGoals = NutritionCalculator.calculateDailyGoals(draftProfile)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            LinearProgressIndicator(
                progress = { (step + 1) / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            when (step) {
                0 -> {
                    Image(
                        painter = painterResource(id = R.drawable.img_onboarding_welcome),
                        contentDescription = strings.onboardingWelcomeTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop
                    )
                    Text(strings.onboardingWelcomeTitle, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        strings.onboardingWelcomeSubtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(strings.selectLanguage, style = MaterialTheme.typography.titleMedium)
                    LanguageSelectorRow(
                        selected = initialProfile.language,
                        onSelect = onLanguageSelected
                    )
                    InfoNoticeBanner(
                        icon = Icons.Outlined.Info,
                        body = strings.medicalDisclaimerShort
                    )
                }

                1 -> {
                    Text(strings.onboardingStepMetrics, style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.fullNameLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { c -> c.isDigit() } },
                            label = { Text(strings.ageLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_age_input")
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it.filter { c -> c.isDigit() } },
                            label = { Text(strings.heightLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_height_input")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currentWeightText,
                            onValueChange = { currentWeightText = it },
                            label = { Text(strings.currentWeightLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_weight_input")
                        )
                        OutlinedTextField(
                            value = goalWeightText,
                            onValueChange = { goalWeightText = it },
                            label = { Text(strings.goalWeightLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_goal_weight_input")
                        )
                    }
                    Text(strings.sexLabel, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BiologicalSex.entries.forEach { option ->
                            FilterChip(
                                selected = sex == option,
                                onClick = { sex = option },
                                label = { Text(strings.sexName(option)) }
                            )
                        }
                    }
                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                2 -> {
                    Text(strings.onboardingStepGoal, style = MaterialTheme.typography.headlineMedium)
                    Text(strings.activityLevelLabel, style = MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActivityLevel.entries.forEach { lvl ->
                            FilterChip(
                                selected = activity == lvl,
                                onClick = { activity = lvl },
                                label = { Text(strings.activityName(lvl)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(strings.nutritionGoalLabel, style = MaterialTheme.typography.titleSmall)
                    val isMinor = parsedAge in 1..17
                    if (isMinor) {
                        InfoNoticeBanner(
                            icon = Icons.Outlined.HealthAndSafety,
                            body = strings.minorSafetyWarning,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutritionGoal.entries.forEach { g ->
                            val disabledForMinor = isMinor && g == NutritionGoal.GRADUAL_LOSS
                            FilterChip(
                                selected = goal == g && !disabledForMinor,
                                enabled = !disabledForMinor,
                                onClick = { goal = g },
                                label = { Text(strings.goalName(g)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                3 -> {
                    Text(strings.onboardingStepSummary, style = MaterialTheme.typography.headlineMedium)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                strings.calculatedDailyTarget,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${calculatedGoals.calories} ${strings.kcal}",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${strings.proteinLabel}: ${calculatedGoals.proteinG}${strings.gramsUnit}  •  " +
                                    "${strings.carbsLabel}: ${calculatedGoals.carbsG}${strings.gramsUnit}  •  " +
                                    "${strings.fatLabel}: ${calculatedGoals.fatG}${strings.gramsUnit}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${strings.bmrLabel}: ${calculatedGoals.bmrKcal} ${strings.kcal} • " +
                                    "${strings.tdeeLabel}: ${calculatedGoals.tdeeKcal} ${strings.kcal}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                    if (calculatedGoals.isMinorProtectionActive) {
                        InfoNoticeBanner(
                            icon = Icons.Outlined.HealthAndSafety,
                            body = strings.minorSafetyWarning,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    InfoNoticeBanner(
                        icon = Icons.Outlined.Info,
                        body = strings.medicalDisclaimerShort
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_back_btn")
                    ) {
                        Text(strings.back)
                    }
                }
                Button(
                    onClick = {
                        if (step == 1) {
                            when {
                                parsedAge !in 10..110 -> {
                                    validationError = strings.validationInvalidAge
                                    return@Button
                                }
                                parsedHeight !in 100..240 -> {
                                    validationError = strings.validationInvalidHeight
                                    return@Button
                                }
                                parsedCurrentWeight < 25.0 || parsedCurrentWeight > 300.0 -> {
                                    validationError = strings.validationInvalidWeight
                                    return@Button
                                }
                                else -> validationError = null
                            }
                        }
                        if (step < 3) {
                            step += 1
                        } else {
                            onComplete(draftProfile.copy(nutritionGoal = calculatedGoals.effectiveGoal))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onboarding_next_btn")
                ) {
                    Text(if (step < 3) strings.next else strings.finish)
                }
            }
        }
        }
    }
}
