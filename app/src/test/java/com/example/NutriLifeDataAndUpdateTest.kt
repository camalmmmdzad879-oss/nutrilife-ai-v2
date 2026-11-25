package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NutriLifeDatabase
import com.example.data.local.RecipeEntity
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.remote.OpenFoodFactsApi
import com.example.data.remote.OpenFoodFactsResponse
import com.example.data.repo.BarcodeLookupResult
import com.example.data.repo.NutriLifeRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.ExerciseType
import com.example.domain.model.MealType
import com.example.domain.model.UserProfile
import com.example.export.AppShareManager
import com.example.i18n.AzStrings
import com.example.i18n.EnStrings
import com.example.i18n.RuStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NutriLifeDataAndUpdateTest {

    private lateinit var context: Context
    private lateinit var db: NutriLifeDatabase
    private lateinit var repo: NutriLifeRepository

    private var simulateNetworkOffline = false

    private val fakeApi = object : OpenFoodFactsApi {
        override suspend fun getProduct(barcode: String): OpenFoodFactsResponse {
            if (simulateNetworkOffline) {
                throw IOException("Internet access unavailable")
            }
            return OpenFoodFactsResponse(status = 0, product = null)
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        simulateNetworkOffline = false
        db = Room.inMemoryDatabaseBuilder(context, NutriLifeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NutriLifeRepository(
            foodDao = db.foodDao(),
            diaryDao = db.diaryDao(),
            waterDao = db.waterDao(),
            weightDao = db.weightDao(),
            exerciseDao = db.exerciseDao(),
            recipeDao = db.recipeDao(),
            savedMealPlanDao = db.savedMealPlanDao(),
            openFoodFactsApi = fakeApi
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun foodDatabaseSeed_containsAzerbaijaniDishesAndPersistsOfflineDiaryWaterWeightExercise() = runTest {
        repo.ensureSeeded()
        val foods = repo.searchFoods("", null, false).first()
        assertTrue(foods.size >= 40)
        assertTrue(foods.any { it.nameAz.contains("Plov", ignoreCase = true) })
        assertTrue(foods.any { it.nameAz.contains("Dolma", ignoreCase = true) })
        assertTrue(foods.any { it.nameAz.contains("Düşbərə", ignoreCase = true) })
        assertTrue(foods.any { it.nameAz.contains("Qutab", ignoreCase = true) })
        assertTrue(foods.any { it.nameAz.contains("Dovğa", ignoreCase = true) })

        val plov = foods.first { it.nameAz.contains("Plov", ignoreCase = true) }
        repo.logFoodToDiary("2026-11-24", MealType.LUNCH, plov, 250.0, "1 plate", plov.nameAz)
        repo.addWater("2026-11-24", 300)
        repo.addWeight("2026-11-24", 68.5, "Morning")
        repo.addExercise("2026-11-24", ExerciseType.WALKING, 30, 68.5, null, "Park")

        val entries = repo.observeDiaryByDate("2026-11-24").first()
        assertEquals(1, entries.size)
        assertEquals(plov.nameAz, entries.first().foodName)
        assertTrue(entries.first().calories > 400.0)
        assertEquals(300, repo.observeWaterByDate("2026-11-24").first().sumOf { it.amountMl })
        assertEquals(68.5, repo.observeWeightHistory().first().first().weightKg, 0.01)
        assertTrue(repo.observeExerciseByDate("2026-11-24").first().first().estimatedCaloriesBurned > 0)
    }

    @Test
    fun barcodeLookup_handlesOfflineLocalHitAndNetworkUnavailableErrorGracefully() = runTest {
        repo.ensureSeeded()
        // Seeded packaged item should resolve offline from Room without calling OpenFoodFacts
        val localFood = repo.searchFoods("", null, false).first().firstOrNull { !it.barcode.isNullOrBlank() }
        if (localFood != null) {
            simulateNetworkOffline = true
            val offlineFound = repo.lookupBarcode(localFood.barcode!!)
            assertTrue(offlineFound is BarcodeLookupResult.Found)
            assertFalse((offlineFound as BarcodeLookupResult.Found).fromOnline)
        }

        // Unknown barcode when internet is unavailable must return NetworkOrOfflineError cleanly
        simulateNetworkOffline = true
        val offlineError = repo.lookupBarcode("9990001112223")
        assertTrue(offlineError is BarcodeLookupResult.NetworkOrOfflineError)
        assertTrue((offlineError as BarcodeLookupResult.NetworkOrOfflineError).message.contains("Internet"))
    }

    @Test
    fun persistentRoomDatabase_survivesSimulatedAppUpdateAndRecognizesNewVersionCode() = runTest {
        val dbName = "nutrilife_upgrade_test_db"
        context.deleteDatabase(dbName)

        val dbV1 = Room.databaseBuilder(context, NutriLifeDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val repoV1 = NutriLifeRepository(
            dbV1.foodDao(),
            dbV1.diaryDao(),
            dbV1.waterDao(),
            dbV1.weightDao(),
            dbV1.exerciseDao(),
            dbV1.recipeDao(),
            dbV1.savedMealPlanDao(),
            fakeApi
        )
        repoV1.ensureSeeded()
        val food = repoV1.searchFoods("Dolma", null, false).first().first()
        repoV1.logFoodToDiary("2026-11-24", MealType.DINNER, food, 220.0, "1 serving", food.nameAz)
        repoV1.saveRecipe(
            RecipeEntity(
                name = "Ev Şorbası",
                servings = 4,
                ingredientsSummary = "Mərci, soğan",
                totalGrams = 800.0,
                totalCalories = 640.0,
                totalProteinG = 36.0,
                totalCarbsG = 90.0,
                totalFatG = 12.0,
                totalFiberG = 18.0
            )
        )
        dbV1.close()

        val prefs = UserPreferencesRepository(context)
        prefs.saveUserProfile(
            UserProfile(
                name = "Nigar",
                age = 26,
                heightCm = 165,
                currentWeightKg = 60.0,
                goalWeightKg = 57.0,
                language = AppLanguage.AZ,
                onboardingCompleted = true
            )
        )
        prefs.recordInstalledVersionCode(1)

        // Simulate upgrade to current BuildConfig.VERSION_CODE (2)
        val prevCode = prefs.lastVersionCodeFlow.first()
        assertTrue(prevCode in 1 until BuildConfig.VERSION_CODE)
        prefs.recordInstalledVersionCode(BuildConfig.VERSION_CODE)

        val dbV2 = Room.databaseBuilder(context, NutriLifeDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val preservedEntries = dbV2.diaryDao().getAllEntriesOnce()
        val preservedRecipes = dbV2.recipeDao().observeAll().first()
        assertEquals(1, preservedEntries.size)
        assertEquals(food.nameAz, preservedEntries.first().foodName)
        assertEquals(1, preservedRecipes.size)
        assertEquals("Ev Şorbası", preservedRecipes.first().name)
        dbV2.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun versionManagement_andUpdateRecognitionPreservePreferencesAndData() = runTest {
        assertTrue(BuildConfig.VERSION_CODE >= 2)
        assertEquals("1.1.0", BuildConfig.VERSION_NAME)
        assertEquals("Camal Memmedzade", BuildConfig.DEVELOPER_NAME)

        val prefs = UserPreferencesRepository(context)
        val initialProfile = UserProfile(
            name = "Aysel",
            age = 27,
            heightCm = 166,
            currentWeightKg = 62.0,
            goalWeightKg = 58.0,
            language = AppLanguage.AZ,
            onboardingCompleted = true
        )
        prefs.saveUserProfile(initialProfile)
        prefs.recordInstalledVersionCode(1)

        val previousCode = prefs.lastVersionCodeFlow.first()
        assertTrue(previousCode in 1 until BuildConfig.VERSION_CODE)

        prefs.recordInstalledVersionCode(BuildConfig.VERSION_CODE)
        val loadedProfile = prefs.userProfileFlow.first()
        assertEquals("Aysel", loadedProfile.name)
        assertEquals(27, loadedProfile.age)
        assertTrue(loadedProfile.onboardingCompleted)
        assertEquals(BuildConfig.VERSION_CODE, prefs.lastVersionCodeFlow.first())

        // Language switch preserves existing user data
        prefs.updateLanguage(AppLanguage.RU)
        val afterLangSwitch = prefs.userProfileFlow.first()
        assertEquals(AppLanguage.RU, afterLangSwitch.language)
        assertEquals("Aysel", afterLangSwitch.name)
        assertEquals(62.0, afterLangSwitch.currentWeightKg, 0.001)
    }

    @Test
    fun shareManager_andFileProvider_buildSafeShareIntentsWithoutPrivatePaths() {
        val linkChooser = AppShareManager.shareTextOrPlayLinkIntent(
            appName = "NutriLife AI",
            inviteText = AzStrings.shareAppInviteText,
            chooserTitle = AzStrings.shareAppTitle
        )
        assertEquals(Intent.ACTION_CHOOSER, linkChooser.action)
        @Suppress("DEPRECATION")
        val targetIntent = linkChooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent?.action)
        assertEquals("text/plain", targetIntent?.type)
        assertTrue(targetIntent?.getStringExtra(Intent.EXTRA_TEXT)?.contains(BuildConfig.VERSION_NAME) == true)

        val apkDir = File(context.cacheDir, "shared_apk").apply { mkdirs() }
        val dummyApk = File(apkDir, "NutriLifeAI-test.apk").apply { writeText("apk-bytes") }
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            dummyApk
        )
        assertEquals("content", contentUri.scheme)
        assertFalse(contentUri.toString().startsWith("file://"))

        val apkChooser = AppShareManager.buildApkShareIntent(
            apkUri = contentUri,
            appName = "NutriLife AI",
            inviteText = EnStrings.shareAppInviteText,
            chooserTitle = EnStrings.shareAppTitle
        )
        assertEquals(Intent.ACTION_CHOOSER, apkChooser.action)
        @Suppress("DEPRECATION")
        val apkSendIntent = apkChooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertEquals("application/vnd.android.package-archive", apkSendIntent?.type)
        assertEquals(contentUri, apkSendIntent?.getParcelableExtra(Intent.EXTRA_STREAM))
        assertTrue((apkSendIntent?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun allThreeLanguages_haveRequiredTranslationsAndExactDeveloperName() {
        listOf(AzStrings, EnStrings, RuStrings).forEach { strings ->
            assertEquals("NutriLife AI", strings.appName)
            assertTrue(strings.copyrightText.contains("Camal Memmedzade"))
            assertTrue(strings.homeGreeting.isNotBlank())
            assertTrue(strings.shareAppTitle.isNotBlank())
            assertTrue(strings.aboutTitle.isNotBlank())
        }
    }
}
