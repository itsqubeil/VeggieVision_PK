package com.veggievision.lokatani

// Import statements yang diperlukan untuk testing
// Tambahkan di bagian atas file test Anda

import android.Manifest
import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Import untuk aplikasi Anda
import com.veggievision.lokatani.R
import com.veggievision.lokatani.view.HostActivity
import com.veggievision.lokatani.view.ResultActivity
import org.junit.Before

@RunWith(AndroidJUnit4::class)
@LargeTest
class CameraFragmentTest {
    @Before
    fun setUp() {
        onView(withId(R.id.cameraFragment)).perform(click())
    }
    @get:Rule
    val activityRule = ActivityScenarioRule(HostActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    @Test
    fun cameraViewandScanTestSaved() {
        // Wait for camera to initialize
        Thread.sleep(2000)

        // Verify camera preview is visible
        onView(withId(R.id.view_finder))
            .check(matches(isDisplayed()))


        // Click capture button
        onView(withId(R.id.captureButton))
            .check(matches(isDisplayed()))
            .perform(click())


        Thread.sleep(5000)
        // Verify we're now in ResultActivity
        onView(withId(R.id.imageView))
            .check(matches(isDisplayed()))

        onView(withId(R.id.autoCompleteTextViewClassName))
            .perform(click())
            .perform(replaceText("bayam"))

        // Clear the weight field and enter "12"
        onView(withId(R.id.editTextRecognizedText))
            .perform(clearText(), typeText("12"))

        // Close keyboard
        closeSoftKeyboard()

        // Verify the gram label is displayed
        onView(withId(R.id.tv_gram))
            .check(matches(isDisplayed()))
            .check(matches(withText("gram")))

        // Click save button
        onView(withId(R.id.buttonSave))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify save action completed (you might want to add verification
        // based on your app's behavior after save, like toast message or navigation)
    }

    @Test
    fun scanCancelTest() {
        // Perform capture first
        Thread.sleep(2000)

        onView(withId(R.id.captureButton))
            .perform(click())

        Thread.sleep(5000)

        // In ResultActivity, click cancel button
        onView(withId(R.id.buttonCancel))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify we're back to camera (or wherever cancel should navigate)
        onView(withId(R.id.view_finder))
            .check(matches(isDisplayed()))
    }







    // Custom ViewAction to wait for view to be gone
    private fun waitForViewToBeGone(timeout: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isDisplayed()
            }

            override fun getDescription(): String {
                return "Wait for view to be gone"
            }

            override fun perform(uiController: UiController, view: View) {
                val endTime = System.currentTimeMillis() + timeout
                while (System.currentTimeMillis() < endTime) {
                    if (view.visibility != View.VISIBLE) {
                        return
                    }
                    uiController.loopMainThreadForAtLeast(100)
                }
                throw AssertionError("View is still visible after $timeout ms")
            }
        }
    }
}