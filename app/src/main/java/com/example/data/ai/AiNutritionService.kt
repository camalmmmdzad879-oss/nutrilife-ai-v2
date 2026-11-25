package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.example.data.local.FoodDao
import com.example.domain.model.AiMealScanResult
import com.example.domain.model.AppLanguage
import com.example.domain.model.FoodCategory
import com.example.domain.model.GeneratedMealPlan
import com.example.domain.model.MealType
import com.example.domain.model.NutrientTotals
import com.example.domain.model.NutritionGoals
import com.example.domain.model.PlannedMeal
import com.example.domain.model.ScannedFoodCandidate
import com.example.domain.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "FirebaseAiNutrition"
internal const val PRIMARY_GEMINI_MODEL = "gemini-3.8-flash"
internal const val FALLBACK_GEMINI_MODEL = "gemini-3.5-flash-lite"
private const val PRIMARY_ATTEMPT_TIMEOUT_MS = 18_000L
private const val FALLBACK_ATTEMPT_TIMEOUT_MS = 18_000L
private const val AI_TOTAL_TIMEOUT_MS = 42_000L
private const val RETRY_DELAY_MS = 700L
internal const val MAX_AI_IMAGE_DIMENSION_PX = 896
internal const val MAX_AI_IMAGE_JPEG_BYTES = 400_000

interface AiNutritionService {
    val isLiveGeminiEnabled: Boolean

    suspend fun analyzeFoodImage(
        imageUri: Uri,
        language: AppLanguage
    ): Result<AiMealScanResult>

    suspend fun generateMealPlan(
        profile: UserProfile,
        goals: NutritionGoals,
        language: AppLanguage,
        variantIndex: Int = 0
    ): Result<GeneratedMealPlan>

    fun generateDailyNutritionInsightStream(
        profile: UserProfile,
        goals: NutritionGoals,
        totals: NutrientTotals,
        waterTodayMl: Int,
        mealsSummary: String,
        language: AppLanguage
    ): Flow<String>
}

class FirebaseAiNutritionService(
    private val context: Context
) : AiNutritionService {

    override val isLiveGeminiEnabled: Boolean = true

    private val foodVisionSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "hiddenIngredientsNote" to Schema.string(description = "Short warning about unmeasured cooking oil, butter, salt, or sauces."),
                "items" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "name" to Schema.string(description = "Localized dish or food name"),
                            "category" to Schema.enumeration(
                                values = FoodCategory.entries.map { it.name },
                                description = "Food category"
                            ),
                            "portionLabel" to Schema.string(description = "Household portion label"),
                            "estimatedGrams" to Schema.double(description = "Estimated weight in grams"),
                            "minGrams" to Schema.double(description = "Lower bound weight in grams"),
                            "maxGrams" to Schema.double(description = "Upper bound weight in grams"),
                            "caloriesPer100g" to Schema.double(description = "Calories per 100g"),
                            "proteinPer100g" to Schema.double(description = "Protein grams per 100g"),
                            "carbsPer100g" to Schema.double(description = "Carbohydrate grams per 100g"),
                            "fatPer100g" to Schema.double(description = "Fat grams per 100g"),
                            "fiberPer100g" to Schema.double(description = "Fiber grams per 100g"),
                            "sugarPer100g" to Schema.double(description = "Sugar grams per 100g"),
                            "sodiumMgPer100g" to Schema.double(description = "Sodium mg per 100g"),
                            "calciumMgPer100g" to Schema.double(description = "Calcium mg per 100g"),
                            "ironMgPer100g" to Schema.double(description = "Iron mg per 100g"),
                            "potassiumMgPer100g" to Schema.double(description = "Potassium mg per 100g"),
                            "vitaminCMgPer100g" to Schema.double(description = "Vitamin C mg per 100g"),
                            "confidenceNote" to Schema.string(description = "Short note about portion estimation uncertainty")
                        ),
                        optionalProperties = listOf("calciumMgPer100g", "ironMgPer100g", "potassiumMgPer100g", "vitaminCMgPer100g", "confidenceNote")
                    )
                )
            )
        )
    }

    private val mealPlanSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Localized meal plan title"),
                "note" to Schema.string(description = "Short non-medical personalization note"),
                "meals" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "mealType" to Schema.enumeration(
                                values = MealType.entries.map { it.name },
                                description = "Meal slot"
                            ),
                            "title" to Schema.string(description = "Dish name in requested language"),
                            "ingredientsText" to Schema.string(description = "Concise ingredients and portion summary"),
                            "portionGrams" to Schema.double(description = "Serving weight in grams"),
                            "calories" to Schema.integer(description = "Estimated kcal for this serving"),
                            "proteinG" to Schema.double(description = "Protein grams"),
                            "carbsG" to Schema.double(description = "Carbs grams"),
                            "fatG" to Schema.double(description = "Fat grams"),
                            "isAzerbaijani" to Schema.boolean(description = "True if Azerbaijani dish")
                        )
                    )
                )
            )
        )
    }

    private val foodVisionPrimaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = foodVisionSchema
            }
        )
    }

    private val foodVisionFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = foodVisionSchema
            }
        )
    }

    private val mealPlanPrimaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mealPlanSchema
            }
        )
    }

    private val mealPlanFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mealPlanSchema
            }
        )
    }

    private val insightPrimaryModel by lazy {
        Firebase.ai.generativeModel(modelName = PRIMARY_GEMINI_MODEL)
    }

    private val insightFallbackModel by lazy {
        Firebase.ai.generativeModel(modelName = FALLBACK_GEMINI_MODEL)
    }

    override suspend fun analyzeFoodImage(
        imageUri: Uri,
        language: AppLanguage
    ): Result<AiMealScanResult> = withContext(Dispatchers.IO) {
        val bitmap = decodeAndOptimizeBitmap(imageUri)
            ?: return@withContext Result.failure(
                IllegalArgumentException(toUserFriendlyAiErrorMessage(IllegalArgumentException("unreadable_image"), language))
            )
        try {
            val langName = languageInstructionName(language)
            val prompt = """
                Analyze this food photo for a nutrition tracker. Respond strictly in $langName.
                Identify every distinct food or dish on the plate (including Azerbaijani dishes such as Plov, Dolma, Qutab, Piti, Düşbərə, Kabab, Dovğa, Təndir çörəyi, Çoban salatı if visible).
                Provide realistic portion gram ranges (minGrams, estimatedGrams, maxGrams) and per-100g nutrition estimates.
                Include a concise hiddenIngredientsNote warning that oil, butter, sauces, and salt cannot be precisely measured from photos.
            """.trimIndent()

            val requestContent = content {
                image(bitmap)
                text(prompt)
            }

            val jsonText = executeWithFastFallback(
                operationTag = "FoodPhotoScan",
                primaryCall = {
                    val response = foodVisionPrimaryModel.generateContent(requestContent)
                    response.text ?: throw IllegalStateException("Empty AI food analysis response")
                },
                fallbackCall = {
                    val response = foodVisionFallbackModel.generateContent(requestContent)
                    response.text ?: throw IllegalStateException("Empty AI food analysis response")
                }
            )
            val parsed = parseMealScanJson(jsonText)
            if (parsed.items.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException(emptyPlateMessage(language))
                )
            }
            Result.success(parsed)
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.e(TAG, "Food image analysis timed out", e)
                Result.failure(Exception(toUserFriendlyAiErrorMessage(e, language), e))
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Food image analysis failed", e)
            Result.failure(Exception(toUserFriendlyAiErrorMessage(e, language), e))
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    override suspend fun generateMealPlan(
        profile: UserProfile,
        goals: NutritionGoals,
        language: AppLanguage,
        variantIndex: Int
    ): Result<GeneratedMealPlan> = withContext(Dispatchers.IO) {
        try {
            val langName = languageInstructionName(language)
            val allergiesInfo = profile.allergies.ifBlank { "None" }
            val prefsInfo = profile.dietaryPreferences.ifBlank { "Balanced" }
            val safetyNote = if (goals.isMinorProtectionActive) {
                "User is under 18: focus strictly on balanced nutrition and healthy habits, never weight-loss deficits."
            } else {
                "Target goal: ${goals.effectiveGoal.name}."
            }
            val prompt = """
                Create a 1-day 4-meal nutrition plan (BREAKFAST, LUNCH, DINNER, SNACK) in $langName.
                Daily calorie target: ~${goals.calories} kcal (Protein ~${goals.proteinG}g, Carbs ~${goals.carbsG}g, Fat ~${goals.fatG}g).
                Allergies to strictly exclude: $allergiesInfo.
                Dietary preferences: $prefsInfo.
                $safetyNote
                Include practical Azerbaijani and international dishes. Variation #$variantIndex.
                Provide non-medical nutrition guidance in the note field.
            """.trimIndent()

            val jsonText = executeWithFastFallback(
                operationTag = "MealPlan",
                primaryCall = {
                    val response = mealPlanPrimaryModel.generateContent(prompt)
                    response.text ?: throw IllegalStateException("Empty AI meal plan response")
                },
                fallbackCall = {
                    val response = mealPlanFallbackModel.generateContent(prompt)
                    response.text ?: throw IllegalStateException("Empty AI meal plan response")
                }
            )
            Result.success(parseMealPlanJson(jsonText))
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.e(TAG, "Meal plan generation timed out", e)
                Result.failure(Exception(toUserFriendlyAiErrorMessage(e, language), e))
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Meal plan generation failed", e)
            Result.failure(Exception(toUserFriendlyAiErrorMessage(e, language), e))
        }
    }

    override fun generateDailyNutritionInsightStream(
        profile: UserProfile,
        goals: NutritionGoals,
        totals: NutrientTotals,
        waterTodayMl: Int,
        mealsSummary: String,
        language: AppLanguage
    ): Flow<String> = flow {
        val langName = languageInstructionName(language)
        val prompt = """
            You are a practical, non-medical nutrition coach inside NutriLife AI.
            Reply in $langName in 3-4 concise bullet points (no medical diagnosis).
            Daily Targets: ${goals.calories} kcal, Protein ${goals.proteinG}g, Carbs ${goals.carbsG}g, Fat ${goals.fatG}g, Fiber ${goals.fiberG}g, Water ${goals.waterMl} ml.
            Today's Intake: ${totals.calories.roundToInt()} kcal, Protein ${totals.proteinG.roundToInt()}g, Carbs ${totals.carbsG.roundToInt()}g, Fat ${totals.fatG.roundToInt()}g, Fiber ${totals.fiberG.roundToInt()}g, Sugar ${totals.sugarG.roundToInt()}g, Sodium ${totals.sodiumMg.roundToInt()}mg, Water $waterTodayMl ml.
            Meals logged today: ${mealsSummary.ifBlank { "No meals logged yet today." }}
            Allergies: ${profile.allergies.ifBlank { "None" }}.
            Give specific, encouraging feedback on remaining macros, hydration, and one practical food suggestion (Azerbaijani or international).
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TOTAL_TIMEOUT_MS) {
                try {
                    withTimeout(PRIMARY_ATTEMPT_TIMEOUT_MS) {
                        insightPrimaryModel.generateContentStream(prompt).collect { chunk ->
                            val piece = chunk.text.orEmpty()
                            if (piece.isNotEmpty()) {
                                accumulated.append(piece)
                                emit(accumulated.toString())
                            }
                        }
                    }
                    if (accumulated.isBlank()) {
                        throw IllegalStateException("Empty primary insight response")
                    }
                } catch (primaryErr: CancellationException) {
                    if (primaryErr is TimeoutCancellationException && accumulated.isBlank()) {
                        Log.w(TAG, "Insight primary timed out; switching immediately to $FALLBACK_GEMINI_MODEL")
                        streamFallbackWithQuickRetry(prompt, accumulated) { emit(it) }
                    } else {
                        throw primaryErr
                    }
                } catch (primaryErr: Exception) {
                    if (accumulated.isNotBlank() || !isRetryableOrOverloadError(primaryErr)) {
                        throw primaryErr
                    }
                    Log.w(TAG, "Insight primary failed (${primaryErr.message}); switching immediately to $FALLBACK_GEMINI_MODEL")
                    streamFallbackWithQuickRetry(prompt, accumulated) { emit(it) }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Empty insight response")
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.e(TAG, "Insight stream timed out", e)
                throw Exception(toUserFriendlyAiErrorMessage(e, language), e)
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Insight generation failed", e)
            throw Exception(toUserFriendlyAiErrorMessage(e, language), e)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun streamFallbackWithQuickRetry(
        prompt: String,
        accumulated: StringBuilder,
        onChunkUpdate: suspend (String) -> Unit
    ) {
        try {
            withTimeout(FALLBACK_ATTEMPT_TIMEOUT_MS) {
                insightFallbackModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        onChunkUpdate(accumulated.toString())
                    }
                }
            }
        } catch (firstFallbackErr: CancellationException) {
            if (firstFallbackErr is TimeoutCancellationException && accumulated.isBlank()) {
                delay(RETRY_DELAY_MS)
                withTimeout(FALLBACK_ATTEMPT_TIMEOUT_MS) {
                    insightFallbackModel.generateContentStream(prompt).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            onChunkUpdate(accumulated.toString())
                        }
                    }
                }
            } else {
                throw firstFallbackErr
            }
        } catch (firstFallbackErr: Exception) {
            if (accumulated.isNotBlank() || !isRetryableOrOverloadError(firstFallbackErr)) {
                throw firstFallbackErr
            }
            delay(RETRY_DELAY_MS)
            withTimeout(FALLBACK_ATTEMPT_TIMEOUT_MS) {
                insightFallbackModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        onChunkUpdate(accumulated.toString())
                    }
                }
            }
        }
    }

    private fun languageInstructionName(language: AppLanguage): String = when (language) {
        AppLanguage.AZ -> "Azerbaijani (Azərbaycan dili)"
        AppLanguage.EN -> "English"
        AppLanguage.RU -> "Russian (Русский)"
    }

    private fun decodeAndOptimizeBitmap(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val maxDim = max(boundsOpts.outWidth, boundsOpts.outHeight)
        if (maxDim <= 0) return null

        var sampleSize = 1
        while (maxDim / sampleSize > MAX_AI_IMAGE_DIMENSION_PX * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val oriented = rotateIfNeeded(decoded, orientation)
        return scaleDownForUpload(oriented, MAX_AI_IMAGE_DIMENSION_PX, MAX_AI_IMAGE_JPEG_BYTES)
    }

    private fun rotateIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    companion object {
        internal suspend fun <T> executeWithFastFallback(
            operationTag: String,
            retryDelayMs: Long = RETRY_DELAY_MS,
            primaryCall: suspend () -> T,
            fallbackCall: suspend () -> T
        ): T {
            return withTimeout(AI_TOTAL_TIMEOUT_MS) {
                try {
                    withTimeout(PRIMARY_ATTEMPT_TIMEOUT_MS) {
                        primaryCall()
                    }
                } catch (primaryErr: CancellationException) {
                    if (primaryErr is TimeoutCancellationException) {
                        Log.w(TAG, "$operationTag primary model ($PRIMARY_GEMINI_MODEL) timed out; switching immediately to $FALLBACK_GEMINI_MODEL")
                        callFallbackWithSingleQuickRetry(operationTag, retryDelayMs, fallbackCall)
                    } else {
                        throw primaryErr
                    }
                } catch (primaryErr: Exception) {
                    if (!isRetryableOrOverloadError(primaryErr)) {
                        throw primaryErr
                    }
                    Log.w(TAG, "$operationTag primary model ($PRIMARY_GEMINI_MODEL) busy/503 (${primaryErr.message}); switching immediately to $FALLBACK_GEMINI_MODEL")
                    callFallbackWithSingleQuickRetry(operationTag, retryDelayMs, fallbackCall)
                }
            }
        }

        private suspend fun <T> callFallbackWithSingleQuickRetry(
            operationTag: String,
            retryDelayMs: Long,
            fallbackCall: suspend () -> T
        ): T {
            try {
                return withTimeout(FALLBACK_ATTEMPT_TIMEOUT_MS) {
                    fallbackCall()
                }
            } catch (firstFallbackErr: CancellationException) {
                if (firstFallbackErr is TimeoutCancellationException) {
                    Log.w(TAG, "$operationTag fallback ($FALLBACK_GEMINI_MODEL) timed out once; retrying once after ${retryDelayMs}ms")
                    delay(retryDelayMs)
                    return withTimeout(FALLBACK_ATTEMPT_TIMEOUT_MS) {
                        fallbackCall()
                    }
                }
                throw firstFallbackErr
            } catch (firstFallbackErr: Exception) {
                if (!isRetryableOrOverloadError(firstFallbackErr)) {
                    throw firstFallbackErr
                }
                Log.w(TAG, "$operationTag fallback ($FALLBACK_GEMINI_MODEL) returned transient error; retrying once after ${retryDelayMs}ms")
                delay(retryDelayMs)
                return withTimeout(FALLBACK_ATTEMPT_TIMEOUT_MS) {
                    fallbackCall()
                }
            }
        }

        internal fun scaleDownForUpload(
            source: Bitmap,
            maxDimensionPx: Int = MAX_AI_IMAGE_DIMENSION_PX,
            maxJpegBytes: Int = MAX_AI_IMAGE_JPEG_BYTES
        ): Bitmap {
            var current = scaleBitmapToMaxDimension(source, maxDimensionPx)
            val stream = ByteArrayOutputStream()
            current.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            if (stream.size() > maxJpegBytes) {
                val tighterMax = (max(current.width, current.height) * 0.72f).roundToInt().coerceAtLeast(384)
                current = scaleBitmapToMaxDimension(current, tighterMax)
            }
            return current
        }

        private fun scaleBitmapToMaxDimension(bitmap: Bitmap, maxDimensionPx: Int): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            val longest = max(width, height)
            if (longest <= maxDimensionPx || width <= 0 || height <= 0) {
                return bitmap
            }
            val scale = maxDimensionPx.toFloat() / longest.toFloat()
            val targetW = (width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (height * scale).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            if (scaled !== bitmap) {
                bitmap.recycle()
            }
            return scaled
        }

        internal fun isRetryableOrOverloadError(error: Throwable): Boolean {
            return generateSequence(error) { it.cause }.any { cause ->
                if (cause is TimeoutCancellationException || cause is SocketTimeoutException) {
                    return@any true
                }
                val className = cause::class.java.simpleName.lowercase()
                if (className.contains("serviceunavailable") ||
                    className.contains("serverexception") ||
                    className.contains("quota") ||
                    className.contains("toomanyrequests") ||
                    className.contains("ioexception")
                ) {
                    return@any true
                }
                val msg = cause.message.orEmpty().lowercase()
                msg.contains("503") ||
                    msg.contains("429") ||
                    msg.contains("high demand") ||
                    msg.contains("overloaded") ||
                    msg.contains("unavailable") ||
                    msg.contains("resource_exhausted") ||
                    msg.contains("resource exhausted") ||
                    msg.contains("rate limit") ||
                    msg.contains("too many requests") ||
                    msg.contains("temporarily") ||
                    msg.contains("try again") ||
                    msg.contains("deadline") ||
                    msg.contains("timed out") ||
                    msg.contains("timeout")
            }
        }

        private fun isNetworkConnectionError(error: Throwable): Boolean {
            return generateSequence(error) { it.cause }.any { cause ->
                if (cause is UnknownHostException || cause is ConnectException || cause is IOException) {
                    val msg = cause.message.orEmpty().lowercase()
                    return@any cause is UnknownHostException ||
                        cause is ConnectException ||
                        msg.contains("network") ||
                        msg.contains("unable to resolve host") ||
                        msg.contains("failed to connect") ||
                        msg.contains("offline") ||
                        msg.contains("socket")
                }
                val msg = cause.message.orEmpty().lowercase()
                msg.contains("unable to resolve host") ||
                    msg.contains("failed to connect") ||
                    msg.contains("network error") ||
                    msg.contains("no address associated")
            }
        }

        private fun isAppCheckOrPermissionError(error: Throwable): Boolean {
            return generateSequence(error) { it.cause }.any { cause ->
                val msg = cause.message.orEmpty().lowercase()
                msg.contains("403") ||
                    msg.contains("appcheck") ||
                    msg.contains("app check") ||
                    msg.contains("attestation") ||
                    msg.contains("permission_denied") ||
                    msg.contains("unauthorized")
            }
        }

        internal fun emptyPlateMessage(language: AppLanguage): String = when (language) {
            AppLanguage.AZ -> "Şəkildə tanına bilən qida tapılmadı. Yeməyi daha yaxşı işıqda və yaxından çəkib yenidən cəhd edin."
            AppLanguage.EN -> "No recognizable food was detected in the photo. Please take a clearer close-up shot and try again."
            AppLanguage.RU -> "На фото не удалось распознать блюда. Сделайте более чёткий снимок при хорошем освещении и повторите попытку."
        }

        internal fun toUserFriendlyAiErrorMessage(error: Throwable, language: AppLanguage): String {
            val causes = generateSequence(error) { it.cause }.toList()
            val isUnreadableImage = causes.any {
                it is IllegalArgumentException && it.message.orEmpty().contains("unreadable_image")
            }
            if (isUnreadableImage) {
                return when (language) {
                    AppLanguage.AZ -> "Seçilən şəkil oxuna bilmədi. Zəhmət olmasa başqa bir aydın qida şəkli seçin."
                    AppLanguage.EN -> "Could not read the selected image. Please choose another clear food photo."
                    AppLanguage.RU -> "Не удалось прочитать выбранное изображение. Пожалуйста, выберите другое чёткое фото."
                }
            }

            if (isAppCheckOrPermissionError(error)) {
                return when (language) {
                    AppLanguage.AZ -> "Tətbiq təhlükəsizlik doğrulaması (App Check) uğursuz oldu. Tətbiqi yenidən başladıb cəhd edin."
                    AppLanguage.EN -> "App security verification (App Check) failed. Please restart the app and try again."
                    AppLanguage.RU -> "Проверка безопасности приложения (App Check) не пройдена. Перезапустите приложение и повторите."
                }
            }

            if (isNetworkConnectionError(error)) {
                return when (language) {
                    AppLanguage.AZ -> "İnternet bağlantısı zəifdir və ya aktiv deyil. Şəbəkəni yoxlayıb yenidən cəhd edin."
                    AppLanguage.EN -> "Internet connection is weak or unavailable. Please check your network and try again."
                    AppLanguage.RU -> "Нет подключения к интернету или сигнал слишком слабый. Проверьте сеть и повторите."
                }
            }

            val isTimeout = causes.any { it is TimeoutCancellationException || it is SocketTimeoutException } ||
                causes.any {
                    val m = it.message.orEmpty().lowercase()
                    m.contains("timed out") || m.contains("timeout")
                }

            val isOverloaded = causes.any {
                val m = it.message.orEmpty().lowercase()
                m.contains("503") || m.contains("429") || m.contains("high demand") ||
                    m.contains("overloaded") || m.contains("unavailable") ||
                    m.contains("resource_exhausted") || m.contains("resource exhausted") ||
                    m.contains("rate limit") || m.contains("too many requests")
            }

            if (isOverloaded) {
                return when (language) {
                    AppLanguage.AZ -> "AI xidmətində hazırda yüksək sıxlıq var (server məşğuldur). Alternativ sürətli model də yoxlanıldı, zəhmət olmasa bir neçə saniyə sonra yenidən cəhd edin."
                    AppLanguage.EN -> "AI service is experiencing high demand right now. Both primary and fast fallback models are busy — please try again in a few seconds."
                    AppLanguage.RU -> "AI-сервис сейчас перегружен высоким спросом. Пожалуйста, повторите попытку через несколько секунд."
                }
            }

            if (isTimeout) {
                return when (language) {
                    AppLanguage.AZ -> "Sorğu gözləniləndən uzun çəkdiyi üçün dayandırıldı. İnternet bağlantınızı yoxlayıb yenidən cəhd edin."
                    AppLanguage.EN -> "AI analysis timed out. Please check your connection and try again."
                    AppLanguage.RU -> "Время ожидания AI-анализа истекло. Проверьте интернет и повторите попытку."
                }
            }

            return when (language) {
                AppLanguage.AZ -> "Qida analizi hazırda tamamlanmadı. Zəhmət olmasa bir neçə saniyə sonra yenidən cəhd edin."
                AppLanguage.EN -> "AI analysis could not be completed right now. Please try again in a few seconds."
                AppLanguage.RU -> "Не удалось завершить AI-анализ. Пожалуйста, повторите попытку через несколько секунд."
            }
        }

        internal fun parseMealScanJson(jsonString: String): AiMealScanResult {
            val root = JSONObject(jsonString)
            val hiddenNote = root.optString("hiddenIngredientsNote", "")
            val itemsArray = root.optJSONArray("items")
            val candidates = buildList {
                if (itemsArray != null) {
                    for (i in 0 until itemsArray.length()) {
                        val obj = itemsArray.getJSONObject(i)
                        val estGrams = obj.optDouble("estimatedGrams", 150.0).coerceIn(10.0, 2000.0)
                        val minG = obj.optDouble("minGrams", estGrams * 0.8).coerceAtLeast(5.0)
                        val maxG = obj.optDouble("maxGrams", estGrams * 1.2).coerceAtLeast(minG)
                        add(
                            ScannedFoodCandidate(
                                id = "gemini_item_${i}_${System.nanoTime()}",
                                name = obj.optString("name", "Food"),
                                category = FoodCategory.fromName(obj.optString("category")),
                                portionLabel = obj.optString("portionLabel", "${estGrams.roundToInt()} g"),
                                estimatedGrams = estGrams,
                                minGrams = minG,
                                maxGrams = maxG,
                                caloriesPer100g = obj.optDouble("caloriesPer100g", 0.0).coerceAtLeast(0.0),
                                proteinPer100g = obj.optDouble("proteinPer100g", 0.0).coerceAtLeast(0.0),
                                carbsPer100g = obj.optDouble("carbsPer100g", 0.0).coerceAtLeast(0.0),
                                fatPer100g = obj.optDouble("fatPer100g", 0.0).coerceAtLeast(0.0),
                                fiberPer100g = obj.optDouble("fiberPer100g", 0.0).coerceAtLeast(0.0),
                                sugarPer100g = obj.optDouble("sugarPer100g", 0.0).coerceAtLeast(0.0),
                                sodiumMgPer100g = obj.optDouble("sodiumMgPer100g", 0.0).coerceAtLeast(0.0),
                                calciumMgPer100g = obj.optDouble("calciumMgPer100g", 0.0).coerceAtLeast(0.0),
                                ironMgPer100g = obj.optDouble("ironMgPer100g", 0.0).coerceAtLeast(0.0),
                                potassiumMgPer100g = obj.optDouble("potassiumMgPer100g", 0.0).coerceAtLeast(0.0),
                                vitaminCMgPer100g = obj.optDouble("vitaminCMgPer100g", 0.0).coerceAtLeast(0.0),
                                confidenceNote = obj.optString("confidenceNote", "")
                            )
                        )
                    }
                }
            }
            return AiMealScanResult(
                items = candidates,
                isPreviewFallback = false,
                uncertaintyNoticeKey = "gemini_live",
                hiddenIngredientsNote = hiddenNote
            )
        }

        internal fun parseMealPlanJson(jsonString: String): GeneratedMealPlan {
            val root = JSONObject(jsonString)
            val title = root.optString("title", "NutriLife AI Plan")
            val note = root.optString("note", "")
            val arr = root.optJSONArray("meals")
            val meals = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        add(
                            PlannedMeal(
                                mealType = MealType.fromName(obj.optString("mealType")),
                                title = obj.optString("title", "Meal"),
                                ingredientsText = obj.optString("ingredientsText", ""),
                                portionGrams = obj.optDouble("portionGrams", 200.0).coerceAtLeast(20.0),
                                calories = obj.optInt("calories", 350).coerceAtLeast(50),
                                proteinG = obj.optDouble("proteinG", 15.0).coerceAtLeast(0.0),
                                carbsG = obj.optDouble("carbsG", 35.0).coerceAtLeast(0.0),
                                fatG = obj.optDouble("fatG", 10.0).coerceAtLeast(0.0),
                                isAzerbaijani = obj.optBoolean("isAzerbaijani", false)
                            )
                        )
                    }
                }
            }
            return GeneratedMealPlan(
                id = "gemini_plan_${System.currentTimeMillis()}",
                title = title,
                meals = meals,
                isPreviewFallback = false,
                note = note
            )
        }
    }
}

class OfflinePreviewAiNutritionService(
    private val context: Context,
    private val foodDao: FoodDao
) : AiNutritionService {

    override val isLiveGeminiEnabled: Boolean = false

    override suspend fun analyzeFoodImage(
        imageUri: Uri,
        language: AppLanguage
    ): Result<AiMealScanResult> = withContext(Dispatchers.IO) {
        try {
            val compressedSizeKb = compressAndValidateImage(imageUri)
            if (compressedSizeKb <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Could not decode selected image."))
            }
            val items = listOf(
                ScannedFoodCandidate(
                    id = "preview_plov",
                    name = when (language) {
                        AppLanguage.AZ -> "Şah Plov (nümunə təxmin)"
                        AppLanguage.EN -> "Shah Plov (sample estimate)"
                        AppLanguage.RU -> "Шах-плов (образец оценки)"
                    },
                    category = FoodCategory.AZERBAIJANI_CUISINE,
                    portionLabel = "1 plate",
                    estimatedGrams = 230.0,
                    minGrams = 190.0,
                    maxGrams = 270.0,
                    caloriesPer100g = 198.0,
                    proteinPer100g = 6.2,
                    carbsPer100g = 24.5,
                    fatPer100g = 8.4,
                    fiberPer100g = 1.4,
                    sugarPer100g = 2.8,
                    sodiumMgPer100g = 340.0
                )
            )
            Result.success(
                AiMealScanResult(
                    items = items,
                    isPreviewFallback = true,
                    uncertaintyNoticeKey = "preview_requires_firebase",
                    hiddenIngredientsNote = "Cooking oil and sauces are estimated."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateMealPlan(
        profile: UserProfile,
        goals: NutritionGoals,
        language: AppLanguage,
        variantIndex: Int
    ): Result<GeneratedMealPlan> = withContext(Dispatchers.IO) {
        val allFoods = foodDao.getTopFoods(20)
        val first = allFoods.firstOrNull()
        val meal = PlannedMeal(
            mealType = MealType.LUNCH,
            title = first?.localizedName(language) ?: "Plov",
            ingredientsText = "250 g",
            portionGrams = 250.0,
            calories = 450,
            proteinG = 18.0,
            carbsG = 52.0,
            fatG = 16.0,
            isAzerbaijani = true
        )
        Result.success(
            GeneratedMealPlan(
                id = "offline_${System.currentTimeMillis()}",
                title = "Local Plan",
                meals = listOf(meal),
                isPreviewFallback = true,
                note = "Offline fallback"
            )
        )
    }

    override fun generateDailyNutritionInsightStream(
        profile: UserProfile,
        goals: NutritionGoals,
        totals: NutrientTotals,
        waterTodayMl: Int,
        mealsSummary: String,
        language: AppLanguage
    ): Flow<String> = flow {
        emit("${totals.calories.roundToInt()} / ${goals.calories} kcal, ${waterTodayMl} / ${goals.waterMl} ml")
    }

    private fun compressAndValidateImage(uri: Uri): Int {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val maxDim = max(boundsOpts.outWidth, boundsOpts.outHeight)
        if (maxDim <= 0) return 0
        val bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return 0
        return try {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            (out.size() / 1024).coerceAtLeast(1)
        } finally {
            bitmap.recycle()
        }
    }
}
