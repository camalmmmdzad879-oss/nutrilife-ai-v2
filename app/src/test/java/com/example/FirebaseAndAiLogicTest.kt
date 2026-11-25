package com.example

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.FirebaseAiNutritionService
import com.example.data.firebase.configureAppCheck
import com.example.domain.calc.NutritionCalculator
import com.example.domain.model.FoodCategory
import com.example.domain.model.MealType
import com.example.domain.model.NutrientTotals
import com.example.domain.model.UserProfile
import com.example.i18n.EnStrings
import com.example.i18n.LocalStrings
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.NutriLifeTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.UiAsyncState
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirebaseAndAiLogicTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun appCheck_persistsDebugTokenSynchronouslyAndFindsRecaptchaSiteKey() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testToken = "test-appcheck-debug-token-123"
        val intent = Intent().putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", testToken)

        configureAppCheck(context, intent)

        val apps = FirebaseApp.getApps(context)
        assertTrue(apps.isNotEmpty())
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "com.google.firebase.appcheck.debug.store.$persistenceKey",
            Context.MODE_PRIVATE
        )
        assertEquals(testToken, prefs.getString("com.google.firebase.appcheck.debug.DEBUG_SECRET", null))

        val resId = context.resources.getIdentifier("recaptcha_site_key", "string", context.packageName)
        assertTrue(resId != 0)
        assertTrue(context.getString(resId).isNotBlank())
    }

    @Test
    fun viewModel_wiresFirebaseAiNutritionServiceWithLiveGeminiEnabled() {
        val viewModel = AppViewModel(composeTestRule.activity.application)
        assertTrue(viewModel.aiService is FirebaseAiNutritionService)
        assertTrue(viewModel.aiService.isLiveGeminiEnabled)
    }

    @Test
    fun structuredGeminiJsonParsers_parseFoodVisionAndMealPlanResponses() {
        val visionJson = """
            {
              "hiddenIngredientsNote": "Butter and oil cannot be measured visually.",
              "items": [
                {
                  "name": "Şah Plov",
                  "category": "AZERBAIJANI_CUISINE",
                  "portionLabel": "1 plate",
                  "estimatedGrams": 240.0,
                  "minGrams": 200.0,
                  "maxGrams": 280.0,
                  "caloriesPer100g": 198.0,
                  "proteinPer100g": 6.2,
                  "carbsPer100g": 24.5,
                  "fatPer100g": 8.4,
                  "fiberPer100g": 1.4,
                  "sugarPer100g": 2.8,
                  "sodiumMgPer100g": 340.0
                }
              ]
            }
        """.trimIndent()

        val scanResult = FirebaseAiNutritionService.parseMealScanJson(visionJson)
        assertFalse(scanResult.isPreviewFallback)
        assertEquals(1, scanResult.items.size)
        assertEquals("Şah Plov", scanResult.items.first().name)
        assertEquals(FoodCategory.AZERBAIJANI_CUISINE, scanResult.items.first().category)
        assertEquals(240.0, scanResult.items.first().estimatedGrams, 0.01)

        val planJson = """
            {
              "title": "Balanced Azerbaijan Plan",
              "note": "Personal tracking only, not medical advice.",
              "meals": [
                {
                  "mealType": "BREAKFAST",
                  "title": "Kükü",
                  "ingredientsText": "Herbs, eggs (180 g)",
                  "portionGrams": 180.0,
                  "calories": 310,
                  "proteinG": 16.0,
                  "carbsG": 6.0,
                  "fatG": 24.0,
                  "isAzerbaijani": true
                },
                {
                  "mealType": "LUNCH",
                  "title": "Yarpaq Dolması",
                  "ingredientsText": "Grape leaves, rice, lamb (220 g)",
                  "portionGrams": 220.0,
                  "calories": 420,
                  "proteinG": 22.0,
                  "carbsG": 28.0,
                  "fatG": 24.0,
                  "isAzerbaijani": true
                }
              ]
            }
        """.trimIndent()

        val plan = FirebaseAiNutritionService.parseMealPlanJson(planJson)
        assertFalse(plan.isPreviewFallback)
        assertEquals(2, plan.meals.size)
        assertEquals(MealType.BREAKFAST, plan.meals[0].mealType)
        assertEquals(730, plan.totalCalories)
    }

    @Test
    fun homeScreen_insightCardShowsLiveBadgeAndSurfacesErrorsTransparently() {
        var requested = false
        val profile = UserProfile(name = "Elvin", onboardingCompleted = true)
        val goals = NutritionCalculator.calculateDailyGoals(profile)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides EnStrings) {
                NutriLifeTheme {
                    HomeScreen(
                        profile = profile,
                        goals = goals,
                        totals = NutrientTotals(calories = 950.0, proteinG = 50.0),
                        waterTodayMl = 1000,
                        diaryEntries = emptyList(),
                        showUpdatedBanner = false,
                        onDismissUpdatedBanner = {},
                        onQuickAddWater = {},
                        onOpenScannerPhoto = {},
                        onOpenScannerBarcode = {},
                        onOpenDiary = {},
                        onOpenPlanner = {},
                        onOpenRecipes = {},
                        onOpenSettings = {},
                        insightState = UiAsyncState.Error("Network unavailable"),
                        isLiveGeminiEnabled = true,
                        onRequestDailyInsight = { requested = true }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_list", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("home_ai_insight_card"))
        composeTestRule.onNodeWithTag("home_ai_insight_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_ai_insight_error").assertTextContains("Network unavailable")
        composeTestRule.onNodeWithTag("home_list", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("home_ai_insight_btn"))
        composeTestRule.onNodeWithTag("home_ai_insight_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(requested)
    }

    @Test
    fun executeWithFastFallback_switchesImmediatelyOn503AndRetriesOnce() = kotlinx.coroutines.test.runTest {
        var primaryCalls = 0
        var fallbackCalls = 0

        val result = FirebaseAiNutritionService.executeWithFastFallback(
            operationTag = "TestVision",
            retryDelayMs = 10L,
            primaryCall = {
                primaryCalls++
                throw IllegalStateException("503 Service Unavailable: high demand")
            },
            fallbackCall = {
                fallbackCalls++
                if (fallbackCalls == 1) {
                    throw IllegalStateException("503 temporarily overloaded")
                }
                "fallback-ok"
            }
        )

        assertEquals(1, primaryCalls)
        assertEquals(2, fallbackCalls)
        assertEquals("fallback-ok", result)
    }

    @Test
    fun scaleDownForUpload_resizesLargeBitmapAndAzErrorMessagesAreLocalized() {
        val large = android.graphics.Bitmap.createBitmap(2400, 1600, android.graphics.Bitmap.Config.RGB_565)
        val scaled = FirebaseAiNutritionService.scaleDownForUpload(large)
        assertTrue(maxOf(scaled.width, scaled.height) <= com.example.data.ai.MAX_AI_IMAGE_DIMENSION_PX)
        if (!scaled.isRecycled) scaled.recycle()

        val highDemandMsg = FirebaseAiNutritionService.toUserFriendlyAiErrorMessage(
            IllegalStateException("503 high demand"),
            com.example.domain.model.AppLanguage.AZ
        )
        assertTrue(highDemandMsg.contains("yüksək sıxlıq", ignoreCase = true))

        val networkMsg = FirebaseAiNutritionService.toUserFriendlyAiErrorMessage(
            java.net.UnknownHostException("generativelanguage.googleapis.com"),
            com.example.domain.model.AppLanguage.AZ
        )
        assertTrue(networkMsg.contains("İnternet", ignoreCase = true))
    }
}
