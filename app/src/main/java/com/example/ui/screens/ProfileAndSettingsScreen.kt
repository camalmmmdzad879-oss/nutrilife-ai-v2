package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.R
import com.example.domain.calc.NutritionCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.BiologicalSex
import com.example.domain.model.NutritionGoal
import com.example.domain.model.ReminderSettings
import com.example.domain.model.UserProfile
import com.example.export.AppShareManager
import com.example.i18n.LocalStrings
import com.example.ui.components.InfoNoticeBanner
import com.example.ui.components.LanguageSelectorRow
import kotlinx.coroutines.launch

@Composable
fun ProfileAndSettingsScreen(
    profile: UserProfile,
    reminderSettings: ReminderSettings,
    onSaveProfile: (UserProfile) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (AppThemeMode) -> Unit,
    onRemindersChanged: (ReminderSettings) -> Unit,
    onExportCsv: () -> Unit,
    onPrepareApkShareUri: suspend () -> android.net.Uri?,
    onOpenAboutPage: () -> Unit,
    onDeleteAllData: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var ageText by remember(profile.age) { mutableStateOf(profile.age.toString()) }
    var heightText by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var currentWeightText by remember(profile.currentWeightKg) { mutableStateOf(profile.currentWeightKg.toString()) }
    var goalWeightText by remember(profile.goalWeightKg) { mutableStateOf(profile.goalWeightKg.toString()) }
    var waterTargetText by remember(profile.dailyWaterTargetMl) { mutableStateOf(profile.dailyWaterTargetMl.toString()) }
    var allergiesText by remember(profile.allergies) { mutableStateOf(profile.allergies) }
    var dietaryPrefsText by remember(profile.dietaryPreferences) { mutableStateOf(profile.dietaryPreferences) }
    var sex by remember(profile.sex) { mutableStateOf(profile.sex) }
    var activity by remember(profile.activityLevel) { mutableStateOf(profile.activityLevel) }
    var goal by remember(profile.nutritionGoal) { mutableStateOf(profile.nutritionGoal) }

    var showShareDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAiInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val parsedAge = ageText.toIntOrNull() ?: profile.age
    val previewProfile = profile.copy(
        name = name.trim(),
        age = parsedAge.coerceIn(10, 120),
        heightCm = (heightText.toIntOrNull() ?: profile.heightCm).coerceIn(100, 240),
        currentWeightKg = (currentWeightText.toDoubleOrNull() ?: profile.currentWeightKg).coerceIn(25.0, 300.0),
        goalWeightKg = (goalWeightText.toDoubleOrNull() ?: profile.goalWeightKg).coerceIn(25.0, 300.0),
        dailyWaterTargetMl = (waterTargetText.toIntOrNull() ?: profile.dailyWaterTargetMl).coerceIn(800, 6000),
        sex = sex,
        activityLevel = activity,
        nutritionGoal = goal,
        allergies = allergiesText.trim(),
        dietaryPreferences = dietaryPrefsText.trim()
    )
    val previewGoals = NutritionCalculator.calculateDailyGoals(previewProfile)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("settings_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(strings.profileTitle, style = MaterialTheme.typography.headlineMedium)
            }

            // Profile Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.editProfileSection, style = MaterialTheme.typography.titleMedium)
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(strings.fullNameLabel) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ageText,
                                onValueChange = { ageText = it.filter(Char::isDigit) },
                                label = { Text(strings.ageLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_age_input")
                            )
                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { heightText = it.filter(Char::isDigit) },
                                label = { Text(strings.heightLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_height_input")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = currentWeightText,
                                onValueChange = { currentWeightText = it },
                                label = { Text(strings.currentWeightLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_weight_input")
                            )
                            OutlinedTextField(
                                value = goalWeightText,
                                onValueChange = { goalWeightText = it },
                                label = { Text(strings.goalWeightLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_goal_weight_input")
                            )
                        }

                        Text(strings.sexLabel, style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BiologicalSex.entries.forEach { s ->
                                FilterChip(
                                    selected = sex == s,
                                    onClick = { sex = s },
                                    label = { Text(strings.sexName(s)) }
                                )
                            }
                        }

                        Text(strings.activityLevelLabel, style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActivityLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = activity == level,
                                    onClick = { activity = level },
                                    label = { Text(strings.activityName(level)) }
                                )
                            }
                        }

                        Text(strings.nutritionGoalLabel, style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            NutritionGoal.entries.forEach { g ->
                                val disabledForMinor = parsedAge < 18 && g == NutritionGoal.GRADUAL_LOSS
                                FilterChip(
                                    selected = goal == g && !disabledForMinor,
                                    onClick = { if (!disabledForMinor) goal = g },
                                    enabled = !disabledForMinor,
                                    label = { Text(strings.goalName(g)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (previewGoals.isMinorProtectionActive) {
                            InfoNoticeBanner(
                                icon = Icons.Outlined.HealthAndSafety,
                                body = strings.minorSafetyWarning,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        OutlinedTextField(
                            value = waterTargetText,
                            onValueChange = { waterTargetText = it.filter(Char::isDigit) },
                            label = { Text(strings.adjustWaterTargetLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = allergiesText,
                            onValueChange = { allergiesText = it },
                            label = { Text(strings.allergiesLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = dietaryPrefsText,
                            onValueChange = { dietaryPrefsText = it },
                            label = { Text(strings.dietaryPrefsLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "${strings.calculatedDailyTarget}: ${previewGoals.calories} ${strings.kcal} (${strings.bmrLabel}: ${previewGoals.bmrKcal} • ${strings.tdeeLabel}: ${previewGoals.tdeeKcal})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = {
                                val validAge = ageText.toIntOrNull()
                                val validHeight = heightText.toIntOrNull()
                                val validWeight = currentWeightText.toDoubleOrNull()
                                when {
                                    validAge == null || validAge !in 10..120 -> onShowMessage(strings.validationInvalidAge)
                                    validHeight == null || validHeight !in 100..240 -> onShowMessage(strings.validationInvalidHeight)
                                    validWeight == null || validWeight !in 25.0..300.0 -> onShowMessage(strings.validationInvalidWeight)
                                    else -> {
                                        onSaveProfile(previewProfile)
                                        onShowMessage(strings.profileSavedMsg)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_save_btn")
                        ) {
                            Text(strings.save)
                        }
                    }
                }
            }

            // Settings Header
            item {
                Text(strings.settingsTitle, style = MaterialTheme.typography.titleLarge)
            }

            // Language & Theme
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(strings.selectLanguage, style = MaterialTheme.typography.titleSmall)
                        LanguageSelectorRow(
                            selected = profile.language,
                            onSelect = onLanguageSelected
                        )

                        HorizontalDivider()

                        Text(strings.themeLabel, style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = profile.themeMode == mode,
                                    onClick = { onThemeSelected(mode) },
                                    label = { Text(strings.themeName(mode)) },
                                    modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Notifications & Reminders
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.remindersTitle, style = MaterialTheme.typography.titleSmall)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            InfoNoticeBanner(
                                icon = Icons.Outlined.Info,
                                body = strings.notificationPermissionNotice
                            )
                            OutlinedButton(
                                onClick = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(strings.grantNotificationPermissionBtn)
                            }
                        }

                        ReminderToggleRow(
                            label = strings.reminderMeals,
                            checked = reminderSettings.mealRemindersEnabled,
                            testTag = "reminder_meals_switch",
                            onCheckedChange = { enabled ->
                                onRemindersChanged(reminderSettings.copy(mealRemindersEnabled = enabled))
                            }
                        )
                        ReminderToggleRow(
                            label = strings.reminderWater,
                            checked = reminderSettings.waterRemindersEnabled,
                            testTag = "reminder_water_switch",
                            onCheckedChange = { enabled ->
                                onRemindersChanged(reminderSettings.copy(waterRemindersEnabled = enabled))
                            }
                        )
                        ReminderToggleRow(
                            label = strings.reminderWeight,
                            checked = reminderSettings.weightRemindersEnabled,
                            testTag = "reminder_weight_switch",
                            onCheckedChange = { enabled ->
                                onRemindersChanged(reminderSettings.copy(weightRemindersEnabled = enabled))
                            }
                        )
                        ReminderToggleRow(
                            label = strings.reminderDiary,
                            checked = reminderSettings.diaryRemindersEnabled,
                            testTag = "reminder_diary_switch",
                            onCheckedChange = { enabled ->
                                onRemindersChanged(reminderSettings.copy(diaryRemindersEnabled = enabled))
                            }
                        )
                    }
                }
            }

            // Share App & About NutriLife AI
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = strings.shareAppTitle,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.shareAppTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = strings.shareAppSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showShareDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_share_app_btn")
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.shareAppTitle, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onOpenAboutPage,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_about_btn")
                            ) {
                                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.aboutTitle, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Data Export, Legal & AI Settings, Delete
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportCsv,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_export_csv_btn")
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.exportCsvBtn)
                        }

                        OutlinedButton(
                            onClick = { showAiInfoDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_ai_info_btn")
                        ) {
                            Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.aiSettingsTitle)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showPrivacyDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_privacy_btn")
                            ) {
                                Icon(Icons.Outlined.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.privacyPolicyTitle, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { showTermsDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_terms_btn")
                            ) {
                                Icon(Icons.Outlined.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.termsOfUseTitle, maxLines = 1)
                            }
                        }

                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_delete_all_btn")
                        ) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.deleteAllDataTitle)
                        }
                    }
                }
            }

            // Dynamic Version Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${strings.appName} • v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${strings.developerLabel}: ${BuildConfig.DEVELOPER_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showShareDialog) {
        ShareOptionsDialog(
            onDismiss = { showShareDialog = false },
            onShareLink = {
                showShareDialog = false
                val intent = AppShareManager.shareTextOrPlayLinkIntent(
                    appName = strings.appName,
                    inviteText = strings.shareAppInviteText,
                    chooserTitle = strings.shareAppTitle
                )
                context.startActivity(intent)
            },
            onShareApk = {
                showShareDialog = false
                scope.launch {
                    val uri = onPrepareApkShareUri()
                    if (uri != null) {
                        val intent = AppShareManager.buildApkShareIntent(
                            apkUri = uri,
                            appName = strings.appName,
                            inviteText = strings.shareAppInviteText,
                            chooserTitle = strings.shareAppTitle
                        )
                        context.startActivity(intent)
                    } else {
                        onShowMessage(strings.shareApkUnavailableMsg)
                        val fallbackIntent = AppShareManager.shareTextOrPlayLinkIntent(
                            appName = strings.appName,
                            inviteText = strings.shareAppInviteText,
                            chooserTitle = strings.shareAppTitle
                        )
                        context.startActivity(fallbackIntent)
                    }
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(strings.privacyPolicyTitle) },
            text = { Text(strings.privacyPolicyBody) },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text(strings.close) } }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text(strings.termsOfUseTitle) },
            text = { Text(strings.termsOfUseBody) },
            confirmButton = { TextButton(onClick = { showTermsDialog = false }) { Text(strings.close) } }
        )
    }

    if (showAiInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAiInfoDialog = false },
            title = { Text(strings.aiSettingsTitle) },
            text = { Text(strings.aiSettingsBody) },
            confirmButton = { TextButton(onClick = { showAiInfoDialog = false }) { Text(strings.close) } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteAllDataTitle) },
            text = { Text(strings.deleteAllDataConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAllData()
                        onShowMessage(strings.dataDeletedMsg)
                    }
                ) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppPage(
    onBack: () -> Unit,
    onPrepareApkShareUri: suspend () -> android.net.Uri?,
    onShowMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showShareDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.aboutTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("about_back_btn")
                    ) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_launcher_emblem),
                            contentDescription = strings.appName,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .testTag("about_app_logo"),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "NutriLife AI",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("about_app_name")
                        )
                        Text(
                            text = "${strings.appVersionLabel}: ${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("about_version_text")
                        )
                        Text(
                            text = "${strings.developerLabel}: ${BuildConfig.DEVELOPER_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("about_developer_text")
                        )
                        Text(
                            text = strings.copyrightText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("about_copyright_text")
                        )
                    }
                }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.aboutTitle, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            text = strings.aboutBody,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("about_description_text")
                        )
                    }
                }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.updateStatusTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = "${strings.packageIdLabel}: ${BuildConfig.APPLICATION_ID}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.testTag("about_package_id_text")
                        )
                        Text(
                            text = strings.updateStatusBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("about_privacy_btn")
                    ) {
                        Icon(Icons.Outlined.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.privacyPolicyTitle, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showTermsDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("about_terms_btn")
                    ) {
                        Icon(Icons.Outlined.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.termsOfUseTitle, maxLines = 1)
                    }
                }

                Button(
                    onClick = { showShareDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_share_app_btn")
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.shareAppTitle)
                }
            }
        }
    }

    if (showShareDialog) {
        ShareOptionsDialog(
            onDismiss = { showShareDialog = false },
            onShareLink = {
                showShareDialog = false
                val intent: Intent = AppShareManager.shareTextOrPlayLinkIntent(
                    appName = strings.appName,
                    inviteText = strings.shareAppInviteText,
                    chooserTitle = strings.shareAppTitle
                )
                context.startActivity(intent)
            },
            onShareApk = {
                showShareDialog = false
                scope.launch {
                    val uri = onPrepareApkShareUri()
                    if (uri != null) {
                        context.startActivity(
                            AppShareManager.buildApkShareIntent(
                                apkUri = uri,
                                appName = strings.appName,
                                inviteText = strings.shareAppInviteText,
                                chooserTitle = strings.shareAppTitle
                            )
                        )
                    } else {
                        onShowMessage(strings.shareApkUnavailableMsg)
                        context.startActivity(
                            AppShareManager.shareTextOrPlayLinkIntent(
                                appName = strings.appName,
                                inviteText = strings.shareAppInviteText,
                                chooserTitle = strings.shareAppTitle
                            )
                        )
                    }
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(strings.privacyPolicyTitle) },
            text = { Text(strings.privacyPolicyBody) },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text(strings.close) } }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text(strings.termsOfUseTitle) },
            text = { Text(strings.termsOfUseBody) },
            confirmButton = { TextButton(onClick = { showTermsDialog = false }) { Text(strings.close) } }
        )
    }
}

@Composable
private fun ShareOptionsDialog(
    onDismiss: () -> Unit,
    onShareLink: () -> Unit,
    onShareApk: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.shareAppTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.shareAppSubtitle, style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = onShareLink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_link_action")
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.shareAppLinkBtn)
                }
                if (!AppShareManager.hasPublishedPlayStoreLink()) {
                    OutlinedButton(
                        onClick = onShareApk,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_apk_action")
                    ) {
                        Icon(Icons.Outlined.Android, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.shareAppApkBtn)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

@Composable
private fun ReminderToggleRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
