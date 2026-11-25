package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.local.FoodItemEntity
import com.example.data.repo.BarcodeLookupResult
import com.example.domain.model.AiMealScanResult
import com.example.domain.model.AppLanguage
import com.example.domain.model.FoodCategory
import com.example.domain.model.MealType
import com.example.domain.model.ScannedFoodCandidate
import com.example.i18n.LocalStrings
import com.example.ui.components.InfoNoticeBanner
import com.example.ui.viewmodel.UiAsyncState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun ScannerScreen(
    initialTab: Int,
    language: AppLanguage,
    aiConsentAccepted: Boolean,
    isLiveGeminiEnabled: Boolean,
    selectedImageUri: Uri?,
    scanState: UiAsyncState<AiMealScanResult>,
    barcodeState: UiAsyncState<BarcodeLookupResult>,
    onAcceptConsent: () -> Unit,
    onAnalyzePhoto: (Uri) -> Unit,
    onUpdateCandidate: (ScannedFoodCandidate) -> Unit,
    onRemoveCandidate: (String) -> Unit,
    onSaveAnalyzedMeal: (MealType) -> Unit,
    onLookupBarcode: (String) -> Unit,
    onLogFood: (FoodItemEntity, MealType, Double, String) -> Unit,
    onAddManualBarcodeFood: (String, String, Double, Double, Double, Double, Double, MealType) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var pendingPhotoAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    var tempCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val uri = tempCaptureUri
            if (success && uri != null) {
                onAnalyzePhoto(uri)
            }
        }
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) onAnalyzePhoto(uri)
        }
    )

    fun launchWithConsent(action: () -> Unit) {
        if (aiConsentAccepted) {
            action()
        } else {
            pendingPhotoAction = action
            showConsentDialog = true
        }
    }

    fun launchCameraCapture() {
        val uri = createTempImageUri(context)
        tempCaptureUri = uri
        takePictureLauncher.launch(uri)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            Text(
                text = strings.scannerTitle,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text(strings.tabPhotoScanner) },
                    modifier = Modifier.testTag("scanner_tab_photo")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text(strings.tabBarcodeScanner) },
                    modifier = Modifier.testTag("scanner_tab_barcode")
                )
            }

            if (activeTab == 0) {
                PhotoScannerTab(
                    selectedImageUri = selectedImageUri,
                    isLiveGeminiEnabled = isLiveGeminiEnabled,
                    scanState = scanState,
                    onTakePhoto = {
                        launchWithConsent {
                            if (hasCameraPermission) {
                                launchCameraCapture()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    onPickGallery = {
                        launchWithConsent {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    onRetryAnalysis = { selectedImageUri?.let(onAnalyzePhoto) },
                    onUpdateCandidate = onUpdateCandidate,
                    onRemoveCandidate = onRemoveCandidate,
                    onSaveAnalyzedMeal = onSaveAnalyzedMeal
                )
            } else {
                BarcodeScannerTab(
                    language = language,
                    hasCameraPermission = hasCameraPermission,
                    barcodeState = barcodeState,
                    onRequestCameraPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onLookupBarcode = onLookupBarcode,
                    onLogFood = onLogFood,
                    onAddManualBarcodeFood = onAddManualBarcodeFood
                )
            }
        }
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text(strings.aiConsentTitle) },
            text = { Text(strings.aiConsentBody) },
            confirmButton = {
                Button(onClick = {
                    onAcceptConsent()
                    showConsentDialog = false
                    pendingPhotoAction?.invoke()
                    pendingPhotoAction = null
                }) {
                    Text(strings.aiConsentAccept)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun PhotoScannerTab(
    selectedImageUri: Uri?,
    isLiveGeminiEnabled: Boolean,
    scanState: UiAsyncState<AiMealScanResult>,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    onRetryAnalysis: () -> Unit,
    onUpdateCandidate: (ScannedFoodCandidate) -> Unit,
    onRemoveCandidate: (String) -> Unit,
    onSaveAnalyzedMeal: (MealType) -> Unit
) {
    val strings = LocalStrings.current
    var editingCandidate by remember { mutableStateOf<ScannedFoodCandidate?>(null) }
    var targetMeal by remember { mutableStateOf(MealType.LUNCH) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isLiveGeminiEnabled) {
            item {
                InfoNoticeBanner(
                    icon = Icons.Outlined.Info,
                    title = strings.aiServiceStatusTitle,
                    body = strings.aiPreviewFallbackNotice,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        item {
            InfoNoticeBanner(
                icon = Icons.Outlined.WarningAmber,
                title = strings.aiAccuracyNoticeTitle,
                body = strings.aiAccuracyNoticeBody
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("photo_take_btn")
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.takePhotoBtn)
                }
                FilledTonalButton(
                    onClick = onPickGallery,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("photo_gallery_btn")
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.pickFromGalleryBtn)
                }
            }
        }

        if (selectedImageUri != null) {
            item {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = strings.scanMealPhotoCta,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(MaterialTheme.shapes.large),
                    contentScale = ContentScale.Crop
                )
            }
        }

        when (scanState) {
            UiAsyncState.Idle -> Unit
            UiAsyncState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiAsyncState.Error -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoNoticeBanner(
                            icon = Icons.Outlined.WarningAmber,
                            body = scanState.message,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (selectedImageUri != null) {
                            OutlinedButton(
                                onClick = onRetryAnalysis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("photo_retry_btn")
                            ) {
                                Text(strings.retry)
                            }
                        }
                    }
                }
            }
            is UiAsyncState.Success -> {
                val result = scanState.data
                item {
                    InfoNoticeBanner(
                        icon = Icons.Outlined.Info,
                        body = "${result.hiddenIngredientsNote} ${strings.hiddenIngredientsWarning}"
                    )
                }
                items(result.items, key = { it.id }) { candidate ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(candidate.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { editingCandidate = candidate }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = strings.edit)
                                }
                                IconButton(onClick = { onRemoveCandidate(candidate.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = strings.delete)
                                }
                            }
                            Text(
                                String.format(
                                    strings.estimatedWeightRangeFormat,
                                    candidate.estimatedGrams.roundToInt(),
                                    candidate.minGrams.roundToInt(),
                                    candidate.maxGrams.roundToInt()
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                String.format(
                                    strings.estimatedCaloriesRangeFormat,
                                    candidate.scaledCalories.roundToInt(),
                                    candidate.calorieRangeMin,
                                    candidate.calorieRangeMax
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${strings.proteinLabel}: ${candidate.scaledProtein.roundToInt()}g • " +
                                    "${strings.carbsLabel}: ${candidate.scaledCarbs.roundToInt()}g • " +
                                    "${strings.fatLabel}: ${candidate.scaledFat.roundToInt()}g • " +
                                    "${strings.fiberLabel}: ${candidate.scaledFiber.roundToInt()}g",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (result.items.isNotEmpty()) {
                    item {
                        Text(strings.selectMealCategory, style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MealType.entries.forEach { m ->
                                FilterChip(
                                    selected = targetMeal == m,
                                    onClick = { targetMeal = m },
                                    label = { Text(strings.mealName(m)) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onSaveAnalyzedMeal(targetMeal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_analyzed_meal_btn")
                        ) {
                            Text(strings.saveAnalyzedMealBtn)
                        }
                    }
                }
            }
        }
    }

    val toEdit = editingCandidate
    if (toEdit != null) {
        var name by remember(toEdit.id) { mutableStateOf(toEdit.name) }
        var gramsText by remember(toEdit.id) { mutableStateOf(toEdit.estimatedGrams.roundToInt().toString()) }
        var kcal100Text by remember(toEdit.id) { mutableStateOf(toEdit.caloriesPer100g.roundToInt().toString()) }

        AlertDialog(
            onDismissRequest = { editingCandidate = null },
            title = { Text(strings.editDetectedItemTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.foodNameLabel) }, singleLine = true)
                    OutlinedTextField(
                        value = gramsText,
                        onValueChange = { gramsText = it },
                        label = { Text(strings.portionGramsLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = kcal100Text,
                        onValueChange = { kcal100Text = it },
                        label = { Text(strings.caloriesPer100gLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val g = gramsText.toDoubleOrNull()?.coerceIn(5.0, 2000.0) ?: toEdit.estimatedGrams
                    val kcal100 = kcal100Text.toDoubleOrNull()?.coerceIn(0.0, 900.0) ?: toEdit.caloriesPer100g
                    onUpdateCandidate(
                        toEdit.copy(
                            name = name.ifBlank { toEdit.name },
                            estimatedGrams = g,
                            minGrams = g * 0.85,
                            maxGrams = g * 1.15,
                            caloriesPer100g = kcal100
                        )
                    )
                    editingCandidate = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { editingCandidate = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun BarcodeScannerTab(
    language: AppLanguage,
    hasCameraPermission: Boolean,
    barcodeState: UiAsyncState<BarcodeLookupResult>,
    onRequestCameraPermission: () -> Unit,
    onLookupBarcode: (String) -> Unit,
    onLogFood: (FoodItemEntity, MealType, Double, String) -> Unit,
    onAddManualBarcodeFood: (String, String, Double, Double, Double, Double, Double, MealType) -> Unit
) {
    val strings = LocalStrings.current
    var manualBarcode by remember { mutableStateOf("") }
    var lastDetectedCode by remember { mutableStateOf("") }
    var showManualEntryDialog by remember { mutableStateOf<String?>(null) }
    var selectedMeal by remember { mutableStateOf(MealType.SNACK) }
    var gramsText by remember { mutableStateOf("100") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.barcodePrompt, style = MaterialTheme.typography.bodyMedium)
        }

        item {
            if (hasCameraPermission) {
                BarcodeCameraPreview(
                    onBarcodeDetected = { code ->
                        if (code != lastDetectedCode) {
                            lastDetectedCode = code
                            manualBarcode = code
                            onLookupBarcode(code)
                        }
                    }
                )
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(36.dp))
                        Text(strings.cameraPermissionRequired, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = onRequestCameraPermission,
                            modifier = Modifier.testTag("barcode_grant_camera_btn")
                        ) {
                            Text(strings.grantCameraPermissionBtn)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.manualBarcodeHint) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("barcode_manual_input")
                )
                Button(
                    onClick = { if (manualBarcode.isNotBlank()) onLookupBarcode(manualBarcode) },
                    modifier = Modifier.testTag("barcode_lookup_btn")
                ) {
                    Text(strings.lookupBarcodeBtn)
                }
            }
        }

        when (barcodeState) {
            UiAsyncState.Idle -> Unit
            UiAsyncState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiAsyncState.Error -> {
                item {
                    InfoNoticeBanner(icon = Icons.Outlined.WarningAmber, body = barcodeState.message)
                }
            }
            is UiAsyncState.Success -> {
                when (val result = barcodeState.data) {
                    is BarcodeLookupResult.Found -> {
                        val food = result.food
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(food.localizedName(language), style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "${food.caloriesPer100g.roundToInt()} ${strings.kcal}/100g • " +
                                            "P ${food.proteinPer100g.roundToInt()}g • C ${food.carbsPer100g.roundToInt()}g • F ${food.fatPer100g.roundToInt()}g",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    OutlinedTextField(
                                        value = gramsText,
                                        onValueChange = { gramsText = it },
                                        label = { Text(strings.portionGramsLabel) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true
                                    )
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        MealType.entries.forEach { m ->
                                            FilterChip(
                                                selected = selectedMeal == m,
                                                onClick = { selectedMeal = m },
                                                label = { Text(strings.mealName(m)) }
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val g = gramsText.toDoubleOrNull()?.coerceIn(1.0, 2000.0) ?: 100.0
                                            onLogFood(food, selectedMeal, g, "${g.roundToInt()} g")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(strings.addFoodToMeal)
                                    }
                                }
                            }
                        }
                    }
                    is BarcodeLookupResult.NotFound,
                    is BarcodeLookupResult.NetworkOrOfflineError -> {
                        val code = when (result) {
                            is BarcodeLookupResult.NotFound -> result.barcode
                            is BarcodeLookupResult.NetworkOrOfflineError -> result.barcode
                            else -> ""
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(strings.barcodeNotFoundTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(strings.barcodeNotFoundBody, style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton(onClick = { showManualEntryDialog = code }) {
                                        Text(strings.addBarcodeManuallyBtn)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val missingCode = showManualEntryDialog
    if (missingCode != null) {
        var name by remember { mutableStateOf("") }
        var kcal by remember { mutableStateOf("200") }
        var p by remember { mutableStateOf("5") }
        var c by remember { mutableStateOf("25") }
        var f by remember { mutableStateOf("8") }
        var g by remember { mutableStateOf("100") }
        AlertDialog(
            onDismissRequest = { showManualEntryDialog = null },
            title = { Text("${strings.addBarcodeManuallyBtn} ($missingCode)") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.foodNameLabel) }, singleLine = true)
                    OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text(strings.caloriesPer100gLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text(strings.proteinPer100gLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text(strings.carbsPer100gLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = f, onValueChange = { f = it }, label = { Text(strings.fatPer100gLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = g, onValueChange = { g = it }, label = { Text(strings.portionGramsLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onAddManualBarcodeFood(
                            missingCode,
                            name.trim(),
                            kcal.toDoubleOrNull() ?: 0.0,
                            p.toDoubleOrNull() ?: 0.0,
                            c.toDoubleOrNull() ?: 0.0,
                            f.toDoubleOrNull() ?: 0.0,
                            g.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0,
                            selectedMeal
                        )
                        showManualEntryDialog = null
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showManualEntryDialog = null }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun BarcodeCameraPreview(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(MaterialTheme.shapes.large),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imgAnalysis ->
                        imgAnalysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(input)
                                    .addOnSuccessListener { barcodes ->
                                        val firstVal = barcodes.firstOrNull()?.rawValue
                                        if (!firstVal.isNullOrBlank()) {
                                            onBarcodeDetected(firstVal)
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun createTempImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "meal_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
