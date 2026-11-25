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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.AppLanguage
import com.example.domain.model.ReminderSettings
import com.example.domain.model.UserProfile
import com.example.i18n.EnStrings
import com.example.i18n.LocalStrings
import com.example.ui.screens.AboutAppPage
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.theme.NutriLifeTheme
import com.example.ui.viewmodel.AppViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun readStringFromContext_returnsNutriLifeAi() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NutriLife AI", appName)
    }

    @Test
    fun mainActivity_launchesCleanlyInRobolectric() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            assertNotNull(activity)
            assertEquals("com.aistudio.nutrilife.kqvmtz", activity.packageName)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun aboutPage_displaysDynamicVersionAndExactDeveloperNameAndShareButton() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides EnStrings) {
                NutriLifeTheme {
                    AboutAppPage(
                        onBack = {},
                        onPrepareApkShareUri = { null },
                        onShowMessage = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("about_app_logo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_app_name").assertTextContains("NutriLife AI")
        composeTestRule.onNodeWithTag("about_version_text")
            .assertTextContains(BuildConfig.VERSION_NAME, substring = true)
        composeTestRule.onNodeWithTag("about_version_text")
            .assertTextContains(BuildConfig.VERSION_CODE.toString(), substring = true)
        composeTestRule.onNodeWithTag("about_developer_text")
            .assertTextContains("Camal Memmedzade", substring = true)
        composeTestRule.onNodeWithTag("about_privacy_btn").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_terms_btn").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_share_app_btn").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settingsShareButton_opensShareOptionsAndLaunchesNativeAndroidSharesheet() {
        var aboutOpened = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStrings provides EnStrings) {
                NutriLifeTheme {
                    ProfileAndSettingsScreen(
                        profile = UserProfile(
                            name = "Kamran",
                            age = 29,
                            heightCm = 176,
                            currentWeightKg = 74.0,
                            goalWeightKg = 70.0,
                            language = AppLanguage.EN,
                            onboardingCompleted = true
                        ),
                        reminderSettings = ReminderSettings(),
                        onSaveProfile = {},
                        onLanguageSelected = {},
                        onThemeSelected = {},
                        onRemindersChanged = {},
                        onExportCsv = {},
                        onPrepareApkShareUri = { null },
                        onOpenAboutPage = { aboutOpened = true },
                        onDeleteAllData = {},
                        onShowMessage = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_about_btn"))
        composeTestRule.onNodeWithTag("settings_about_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(aboutOpened)

        composeTestRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_share_app_btn"))
        composeTestRule.onNodeWithTag("settings_share_app_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("share_link_action").performClick()
        composeTestRule.waitForIdle()

        val nextIntent = Shadows.shadowOf(composeTestRule.activity).nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals(Intent.ACTION_CHOOSER, nextIntent.action)
    }

    @Test
    fun appRoot_onboardingAllNavigationTabsPlannerSettingsShareAndAbout() {
        val viewModel = AppViewModel(composeTestRule.activity.application)
        composeTestRule.setContent {
            NutriLifeAppRoot(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Step 0 -> Step 1 -> Step 2 -> Step 3 -> Finish Onboarding
        composeTestRule.onNodeWithTag("lang_chip_en").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_next_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_name_input").performScrollTo().performTextInput("Leyla")
        composeTestRule.onNodeWithTag("onboarding_next_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_next_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_next_btn").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasTestTag("nav_profile")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        // Home quick water + Planner sub-screen + Back
        composeTestRule.onNodeWithTag("home_list", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("home_water_add_250"))
        composeTestRule.onNodeWithTag("home_water_add_250").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_list", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("home_planner_btn"))
        composeTestRule.onNodeWithTag("home_planner_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("planner_back_btn").performClick()
        composeTestRule.waitForIdle()

        // Scanner tab
        composeTestRule.onNodeWithTag("nav_scanner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scanner_tab_barcode").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Diary tab
        composeTestRule.onNodeWithTag("nav_diary").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("diary_add_fab").assertIsDisplayed()

        // Stats tab
        composeTestRule.onNodeWithTag("nav_stats").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stats_tab_0").assertIsDisplayed()

        // Profile/Settings -> About
        composeTestRule.onNodeWithTag("nav_profile").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_list", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("settings_about_btn"))
        composeTestRule.onNodeWithTag("settings_about_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("about_developer_text")
            .assertTextContains("Camal Memmedzade", substring = true)
        composeTestRule.onNodeWithTag("about_version_text")
            .assertTextContains(BuildConfig.VERSION_NAME, substring = true)
    }
}
